const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '.env') });

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');

const app = express();
const port = process.env.PORT || 3000;
const mongoUri = process.env.MONGO_URI || process.env.MONGODB_URI;

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
    return {
        id: song._id,
        title: song.title,
        artist: song.artist,
        duration: song.duration,
        imageUrl: createMediaUrl(req, 'images', String(song.imageFileId)),
        audioUrl: createMediaUrl(req, 'audio', String(song.audioFileId))
    };
}

async function streamFromBucket(bucket, fileId, req, res) {
    const objectId = new mongoose.Types.ObjectId(fileId);
    const files = await bucket.find({ _id: objectId }).toArray();

    if (files.length === 0) return false;

    const file = files[0];
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
            'Content-Type': file.metadata?.contentType || 'application/octet-stream',
        });

        bucket.openDownloadStream(objectId, { start, end: end + 1 }).pipe(res);
    } else {
        res.writeHead(200, {
            'Content-Length': fileSize,
            'Accept-Ranges': 'bytes',
            'Content-Type': file.metadata?.contentType || 'application/octet-stream',
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
        const songs = await Song.find().sort({ createdAt: 1 });
        res.json(songs.map((song) => toSongResponse(req, song)));
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

        const found = await streamFromBucket(mediaBucket, fileId, req, res);

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
