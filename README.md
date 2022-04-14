# COP4520_Assignment3

## Problem 1: The Birthday Presents Party
How to Run:

In order to run this code, please do the following:
  1. Download the Problem1.java or transfer it to the cmd line you will be utilizing
  2. Change directories to the directory containing Problem1.java in the terminal using the cd command 
  3. Once in this directory, run the command: javac Problem1.java
  4. Then, run the command: java Problem1
 
The program will then output its results onto the command line. This method of installation only works for command lines such as Eustis.

Summary of Approach:
I approached this problem using a LockFreeList, such as the one found in the book. Due to our need to have a linked list structure while accessing and making changes to it, a LockFreeList is a strong candidate for this scenario. It utilizes logical removal so as to avoid losing references in the LinkedList, later cleaning up the list so as to improve runtime. At the end of the day this is a LinkedList so traversal through it is inefficient (taking O(n) time in the worst-case scenario). In order to optimize the removal of presents, I removed presents from the front of the list. This sped up the thank you card writing process but realistically does not improve runtime when wanting to remove spcific element N.


## Problem 2: Atmospheric Temperature Reading Module 
How to Run:

In order to run this code, please do the following:
  1. Download the Problem2.java or transfer it to the cmd line you will be utilizing
  2. Change directories to the directory containing Problem2.java in the terminal using the cd command 
  3. Once in this directory, run the command: javac Problem2.java
  4. Then, run the command: java Problem2
 
The program will then output its results onto the command line. This method of installation only works for command lines such as Eustis.

Summary of Approach:
I approached this problem using a set of arrays. First, we have two 2D arrays, prevReadings and currReadings, both of which store the readings for an hour. The important distinction between them is that once the report compilation begins, we switch all our data from currReadings to prevReadings. Though slow, this ensures that we are able to continue taking readings without interfering with the compilation process.  This copying over takes up O(n) time but allows us to continue doing mission critical tasks.
