# lab1ChordDS
Laboratory 1 for asignature Distributed Systems, with focus on Chord code on Peer Sim package

---------------------------------
Chord for the Peersim Simulator
---------------------------------

The jar files of peersim must be downloaded separately.
An example exmaple.cfg configuration file can be found in the 
main directory.

---------------------------------
Makefile
---------------------------------

To run the Makefile, modify the PEERSIM_JARS variable to point to 
your peersim installation, or copy the peersim jar files to the lib
directory. 

To compile the sources, invoke:

  make

To compile the API documentation, invoke:

  make doc

To run the code, invoke:

  make run

To run all the previous command in this order, invoke:

  make all


----------------------------

----------------------------


# COMPILE CHORD EXAMPLE
javac -classpath peersim-1.0.5.jar:djep-1.0.0.jar:jep-2.3.0.jar:classes/ -d classes/ src/peersim/chord/*.java

# EXECUTE CHORD EXAMPLE
java -cp peersim-1.0.5.jar:djep-1.0.0.jar:jep-2.3.0.jar:classes/ peersim.Simulator example.cfg


Collaboratos: Cristian Jeldes - Pablo Reyes