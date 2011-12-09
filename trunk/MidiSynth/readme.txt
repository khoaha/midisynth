==============
||  README  ||
==============

To use the program, first select any of the given generations.
 - Random is the initial data pool consisting solely of random creations.
 - Generation 1 consists of loops formed by observing the seeds outputted by the random set and their ratings.
 - Generation 2 uses data collected in generation 1 and creates new loops based off their sequences and ratings.
 - Generation 3 is to generation 2 as generation 2 is to generation 1.
 
The data we gathered from our human sampling trials is included for the program to use during playback.
 - Random, by nature, uses no source file
 - Gen 1 references "crowd1_sort.txt"
 - Gen 2 references "sample2.txt"
 - Gen 3 references "sample3.txt"
 Modifying these files will alter the results of each generation.
 
The program outputs data to "rates-2.txt" whenever a rating is selected.
Higher rated loops have more influence over the next generation.
The Random generation and the higher generations produce different output formats, and the two cannot be mixed.
To observe the effects of these changes, replace Gen 1's source file contents with the output from Random
Or replace Gen 2 or Gen 3's source files with the output from any other generation.

To play a track, just hit "Play" and then hit "Stop" to halt it.
When a track is halted, the next time "Play" is hit a new track with randomize.

For the random generation, you can set particular seeds to play again using the "Set Seed" feature.
The next time you hit "Play", it will create a sample from that seed instead of re-randomizing.