const path = require('path');
const fs = require('fs');
require('dotenv').config({ path: path.join(__dirname, '.env') });

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');

const app = express();
const port = process.env.PORT || 3000;
const mongoUri = process.env.MONGO_URI || process.env.MONGODB_URI;
const imagesDir = path.join(__dirname, 'public', 'images');
const audioDir = path.join(__dirname, 'public', 'audio');

let mediaBucket = null;

app.use(cors());
app.use(express.json());

const songSchema = new mongoose.Schema(
    {
        title: { type: String, required: true },
        artist: { type: String, required: true },
        duration: { type: Number, required: true },
        imageFileId: { type: mongoose.Schema.Types.ObjectId, required: true },
        audioFileId: { type: mongoose.Schema.Types.ObjectId, required: true }
    },
    { timestamps: true }
);

const Song = mongoose.model('Song', songSchema);

function createMediaUrl(req, mediaType, fileId) {
    return `${req.protocol}://${req.get('host')}/media/${mediaType}/${fileId}`;
}

function toSongResponse(req, song) {
    const imageUrl = createMediaUrl(req, 'images', String(song.imageFileId));
    const audioUrl = createMediaUrl(req, 'audio', String(song.audioFileId));

    return {
        id: song._id,
        title: song.title,
        artist: song.artist,
        duration: song.duration,
        time: song.duration,
        imageUrl,
        audioUrl,
        image: imageUrl,
        audio: audioUrl
    };
}

function parsePositiveInt(value, fallback) {
    const parsed = Number.parseInt(value, 10);

    if (Number.isNaN(parsed) || parsed < 1) {
        return fallback;
    }

    return parsed;
}

function streamLocalFile(filePath, contentType, req, res) {
    const stat = fs.statSync(filePath);
    const fileSize = stat.size;
    const range = req.headers.range;

    if (range) {
        const parts = range.replace(/bytes=/, '').split('-');
        const start = Number.parseInt(parts[0], 10);
        const end = parts[1] ? Number.parseInt(parts[1], 10) : fileSize - 1;
        const chunkSize = (end - start) + 1;

        res.writeHead(206, {
            'Content-Range': `bytes ${start}-${end}/${fileSize}`,
            'Accept-Ranges': 'bytes',
            'Content-Length': chunkSize,
            'Content-Type': contentType,
        });

        fs.createReadStream(filePath, { start, end }).pipe(res);
    } else {
        res.writeHead(200, {
            'Content-Length': fileSize,
            'Accept-Ranges': 'bytes',
            'Content-Type': contentType,
        });
        fs.createReadStream(filePath).pipe(res);
    }

    return true;
}

async function streamFromBucket(bucket, fileId, mediaType, req, res) {
    const objectId = new mongoose.Types.ObjectId(fileId);
    const files = await bucket.find({ _id: objectId }).toArray();

    if (files.length === 0) return false;

    const file = files[0];
    const localDir = mediaType === 'audio' ? audioDir : imagesDir;
    const localPath = path.join(localDir, file.filename);
    const contentType = file.metadata?.contentType || 'application/octet-stream';

    if (fs.existsSync(localPath)) {
        return streamLocalFile(localPath, contentType, req, res);
    }

    const fileSize = file.length;
    const range = req.headers.range;

    if (range) {
        const parts = range.replace(/bytes=/, "").split("-");
        const start = parseInt(parts[0], 10);
        const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;
        const chunksize = (end - start) + 1;

        res.writeHead(206, {
            'Content-Range': `bytes ${start}-${end}/${fileSize}`,
            'Accept-Ranges': 'bytes',
            'Content-Length': chunksize,
            'Content-Type': contentType,
        });

        bucket.openDownloadStream(objectId, { start, end: end + 1 }).pipe(res);
    } else {
        res.writeHead(200, {
            'Content-Length': fileSize,
            'Accept-Ranges': 'bytes',
            'Content-Type': contentType,
        });
        bucket.openDownloadStream(objectId).pipe(res);
    }
    return true;
}

app.get('/health', (req, res) => {
    res.json({ status: 'ok' });
});

app.get('/songs', async (req, res) => {
    try {
        const page = parsePositiveInt(req.query.page, 1);
        const limit = parsePositiveInt(req.query.limit, 6);
        const shouldPaginate = req.query.page !== undefined || req.query.limit !== undefined;

        if (!shouldPaginate) {
            const songs = await Song.find().sort({ createdAt: 1 });
            res.json(songs.map((song) => toSongResponse(req, song)));
            return;
        }

        const total = await Song.countDocuments();
        const skip = (page - 1) * limit;
        const songs = await Song.find()
            .sort({ createdAt: 1 })
            .skip(skip)
            .limit(limit)
            .lean();

        res.json({
            items: songs.map((song) => toSongResponse(req, song)),
            page,
            limit,
            total,
            hasMore: skip + songs.length < total,
            nextPage: skip + songs.length < total ? page + 1 : null
        });
    } catch (error) {
        res.status(500).json({ message: 'Failed to load songs' });
    }
});

app.get('/media/:mediaType/:fileId', async (req, res) => {
    try {
        if (!mediaBucket) {
            res.status(503).json({ message: 'Media store is not ready' });
            return;
        }

        const { mediaType, fileId } = req.params;

        if (mediaType !== 'images' && mediaType !== 'audio') {
            res.status(400).json({ message: 'Invalid media type' });
            return;
        }

        const found = await streamFromBucket(mediaBucket, fileId, mediaType, req, res);

        if (!found) {
            res.status(404).json({ message: 'Media not found' });
        }
    } catch (error) {
        res.status(500).json({ message: 'Failed to load media' });
    }
});

async function startServer() {
    if (!mongoUri) {
        console.error('Missing MongoDB connection string. Set MONGO_URI or MONGODB_URI in .env');
        process.exit(1);
    }

    await mongoose.connect(mongoUri);
    mediaBucket = new mongoose.mongo.GridFSBucket(mongoose.connection.db, {
        bucketName: 'media'
    });

    app.listen(port, () => {
        console.log(`Server running at http://localhost:${port}`);
    });
}

startServer().catch((error) => {
    console.error('Failed to start server:', error.message);
    process.exit(1);
});
