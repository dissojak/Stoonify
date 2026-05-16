const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

const fs = require('fs');
const mongoose = require('mongoose');
const { parseFile } = require('music-metadata');

const mongoUri = process.env.MONGO_URI || process.env.MONGODB_URI;
const imagesDir = path.join(__dirname, '..', 'public', 'images');
const audioDir = path.join(__dirname, '..', 'public', 'audio');

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

const Song = mongoose.models.Song || mongoose.model('Song', songSchema);

const metadataByStem = {
    'Cassö x Raye x D Block Europe - Prada (Official Video)': {
        title: 'Prada',
        artist: 'Cassö x Raye x D-Block Europe'
    },
    'Evermore - It\'s Too Late (Dirty South Remix)': {
        title: "It's Too Late (Dirty South Remix)",
        artist: 'Evermore'
    },
    'Haddaway - What Is Love (Official 4K Video)': {
        title: 'What Is Love',
        artist: 'Haddaway'
    },
    'Home - Adriatique & Marino Canal - (feat. Delhia De France)': {
        title: 'Home',
        artist: 'Adriatique & Marino Canal feat. Delhia De France'
    },
    'Inna - Hot (Official Video HD)': {
        title: 'Hot',
        artist: 'Inna'
    },
    'La Bouche - Be My Lover (Official Video)': {
        title: 'Be My Lover',
        artist: 'La Bouche'
    },
    'Low Deep T Number 1': {
        title: 'Number 1',
        artist: 'Low Deep T'
    },
    'Madonna - 4 Minutes feat. Justin Timberlake & Timbaland': {
        title: '4 Minutes',
        artist: 'Madonna feat. Justin Timberlake & Timbaland'
    },
    'The Avener, Kadebostany - Castle In The Snow (Official Video)': {
        title: 'Castle In The Snow',
        artist: 'The Avener, Kadebostany'
    },
    'On The Floor': {
        title: 'On The Floor',
        artist: 'Jennifer Lopez feat. Pitbull'
    },
    'Yebba - Far Away (Audio) ft. A$AP Rocky': {
        title: 'Far Away',
        artist: 'Yebba ft. A$AP Rocky'
    },
    'La Cross - Save Me ( High Quality )': {
        title: 'Save Me',
        artist: 'La Cross'
    },
    'Madonna - La Isla Bonita (Official Video)': {
        title: 'La Isla Bonita',
        artist: 'Madonna'
    }
};

function listFilesByStem(directory) {
    return fs.readdirSync(directory).reduce((files, fileName) => {
        if (fileName.startsWith('.')) {
            return files;
        }

        const stem = path.parse(fileName).name;
        files.set(stem, path.join(directory, fileName));
        return files;
    }, new Map());
}

function uploadFile(bucket, filePath, fileName, contentType) {
    return new Promise((resolve, reject) => {
        const uploadStream = bucket.openUploadStream(fileName, {
            metadata: {
                contentType,
                originalName: fileName
            }
        });

        fs.createReadStream(filePath)
            .pipe(uploadStream)
            .on('error', reject)
            .on('finish', () => resolve(uploadStream.id));
    });
}

async function getDurationInSeconds(filePath) {
    const metadata = await parseFile(filePath);
    return Math.max(1, Math.round(metadata.format.duration || 0));
}

async function clearCollection(bucket) {
    const existingFiles = await bucket.find({}).toArray();

    for (const file of existingFiles) {
        await bucket.delete(file._id);
    }
}

async function main() {
    if (!mongoUri) {
        throw new Error('Missing MongoDB connection string. Set MONGO_URI or MONGODB_URI in .env');
    }

    await mongoose.connect(mongoUri);
    const bucket = new mongoose.mongo.GridFSBucket(mongoose.connection.db, {
        bucketName: 'media'
    });

    await Song.deleteMany({});
    await clearCollection(bucket);

    const imageFiles = listFilesByStem(imagesDir);
    const audioFiles = listFilesByStem(audioDir);
    const importedSongs = [];

    for (const [stem, audioPath] of audioFiles.entries()) {
        const imagePath = imageFiles.get(stem);

        if (!imagePath) {
            console.warn(`Skipping ${stem} because no matching image file was found.`);
            continue;
        }

        const meta = metadataByStem[stem] || {
            title: stem,
            artist: 'Unknown Artist'
        };

        const duration = await getDurationInSeconds(audioPath);
        const audioId = await uploadFile(bucket, audioPath, path.basename(audioPath), 'audio/mpeg');
        const imageId = await uploadFile(bucket, imagePath, path.basename(imagePath), 'image/jpeg');

        importedSongs.push({
            title: meta.title,
            artist: meta.artist,
            duration,
            imageFileId: imageId,
            audioFileId: audioId
        });
    }

    await Song.insertMany(importedSongs);

    console.log(`Imported ${importedSongs.length} songs into MongoDB Atlas.`);
    await mongoose.disconnect();
}

main().catch(async (error) => {
    console.error('Import failed:', error.message);
    await mongoose.disconnect().catch(() => {});
    process.exit(1);
});