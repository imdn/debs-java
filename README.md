# README

This repository contains my solution coded in Java for the ACM Distributed Event-Based Systems Grand Challenge (DEBS GC) 2017. The problem statement can be viewed [here](https://project-hobbit.eu/challenges/debs-grand-challenge/)

Arriving at the solution involves a good grasp of the following concepts:

- Semantic Web data model
	 - Resource Description Framework (RDF) / Turtle serialization
	 - Triple stores
	 - Ontologies (OWL)
- Machine Learning
	- Probability / Markov models
	- Clustering algorithms (k-means)

The following technologies/frameworks were involved in developing and evaluating the solution:

- Java 8 (collections, streams, threading)
- Apache Maven (build tool)
- RabbitMQ (message broker)
- Docker 

The solution is benchmarked for accuracy and performance on the [HOBBIT](https://project-hobbit.eu/outcomes/hobbit-platform/) platform. Benchmarked results for this solution can be viewed [here](http://master.project-hobbit.eu/experiments/1512696744492,1512696709965,1512696694914) (requires registration on the HOBBIT project).

My alternative implementation in Python 3.6 is available [here](https://github.com/imdn/debs-py)