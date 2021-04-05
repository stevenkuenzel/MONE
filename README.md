# Final Kotlin Implementation

This project represents the resulting implementation of my dissertation. It allows applying nNEAT on all the described scenarios, including the case studies.

Note that the experiments within the thesis were conducted and analysed with the experimental implementation in the project `MONE_KOTLIN`, you can find it on the CD: `Source Code/Experimental Kotlin Implementation`.

However, certain bugs were obtained in `MONE_KOTLIN` when creating this implementation:
* The `AddLink` mutation always added positive link weights.
* The attributes of q-Procedures were not initialized with zero and thereby have always taken an initial value of minus one. It somehow disturbed the ranking in some cases.
* (The `update` procedure of neural networks has been changed towards the procedure defined by K. Stanley as it is the 'more common' one.)

These changes may result in performance differences between the experiments described in the thesis and experiments carried out with this implementation.

### Loading the Project / Preparation
1. Extract the project from the CD to your local disk
2. Open/Import the project within your IDE, e.g., IntelliJ IDEA
   1. Run the Gradle build script
   2. During testing it occurred that the project was not loaded correctly by IntelliJ IDEA. Removing the project and extracting it again from the archive typically solved the problem.
3. To run experiments concerning FightingICE or TORCS (not simTORCS), extract the content of `CD/Source Code/Final Kotlin Implementation/external/FightingICE.z7` (respectively `TORCS.7z`) into the directory `Project root/external`. (Note: For example the file `wtorcs.exe` has to be located in the directory `Project root/external/torcs/wtorcs.exe`.)


### Run Configurations

* Run: Runs a (set of) experiment(s) with the configuration(s) provided in the file `input/config.json` (a default config-file is created automatically, if not existing)
* Postprocess: Evaluates the experimental results (XML-files; these have to be provided in the `input` directory). The resulting files are stored in the `output` directory. Note that the composition of experiments is set in the file `input/analyse.xml` (not created automatically, if not existing).
* Evaluate: Evaluate a single solution (neural network) in a certain scenario. The scenario is defined in `input/config.json`, entry `Experiment`. The network to evaluate has to be stored in the file `input/ANN.xml`.

### Online Sources
The most up-to-date version of this project can be found on GitHub: https://github.com/stevenkuenzel/Dissertation