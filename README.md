# CaveSwingWorkshop


The CaveSwing implementation is taken from https://github.com/ljialin/SimpleAsteroids.

## installation
Python dependencies (It is recommended to create a virtual environment for the project):
- thrift
- numpy
- matplotlib
- python-opencv

optionally: Jupiter notebook, but can work without it

Java
- version 8
- additional libraries are in the lib subfolder

## running Map-elites
The main files for tuning the parameters in CaveSwing can be found in the src/hyperopt package. It contains both python and Java code.

To run the project the following steps should be followed:
- 1, run main in ```TuneMapElites.java```, which starts up the server.
- 2, run either the ```map_elites.ipnyb``` or ```mapElite.py```

You can modify, which behaviour descriptors you want to use.
It is also possibile to add new descriptors, which has to be done on the Java side in the ```TuneMapElites.java```, where the descriptors are added to the ```Results``` object.
