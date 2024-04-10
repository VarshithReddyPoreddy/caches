# For Compiling sim_cache uses make file
JAVAC = javac
#setting source file
SRC = sim_cache.java
#setting main class for sim_cache
MAIN_CLASS = sim_cache
#to save target class
#use make command to compile the sim_cache program
all: $(SRC:.java=.class)
%.class: %.java
	$(JAVAC) $<
clean:
	rm -f *.class
#to remove class files use make clean