# wosu-api
Quick and dirty api for use with the wosu frontend.

This project was built using Eclipse. It unzips all osz files, then parses through all the osu files, then serves them in both json and raw format. Hashes point to either entire songs or individual maps.

## Usage
`java -jar BeatmapAPI.jar <songs folder> <cache file> <cache folder>`
The songs folder contains all beatmaps, much like osu!'s Songs folder. The cache file contains some options and a mapping from hashes to songs and maps. The cache folder contains json metadata, with hashes in the filename.

## TODO
* Replace httpserver implementation from freeutils.net (javaxt-server?)
* Implement background image detection
* Use cached information correctly and detect changes in songs
