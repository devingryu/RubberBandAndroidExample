# About this repository
![screen](https://user-images.githubusercontent.com/79868575/116930012-2758bc80-ac9a-11eb-9959-595dd4e4ad96.png)  

This project is simple Android Example for [RubberBand Library](https://github.com/breakfastquay/rubberband). 

Sample song, which is pitch shifted with ratio 1.2 and sped up with ratio 1.3, will be played automatically when you run this example.
(Note that speed ratio > 1 means it is sped up, unlike RubberBand Library. You might want to put inverse value of ratio you expect to change speed in RubberBand)

ExoPlayer is used to play the audio file, and custom AudioProcessor RubberBandAudioProcessor is added to process audio with RubberBand.  
Copyright information of the sample song is noted below section. For bundled libraries, check [this](https://github.com/breakfastquay/rubberband#5-copyright-notes-for-bundled-libraries).  



# For intergration
You need to build RubberBand with android-ndk first. See [here](https://github.com/breakfastquay/rubberband#4e-building-for-android-and-java-integration) for further information.

Then you need to include `RubberBandStretcher.java` and `RubberBandAudioProcessor.kt` in this project.  
**Note that `RubberBandStretcher.java` is different from the one in RubberBand Library! You should include version of this project to make this correcty operate.**  

Lastly, Create ExoPlayer instance with RubberBandAudioProcessorChain.

# Sample Song Information
Song: Step! by Hybee
