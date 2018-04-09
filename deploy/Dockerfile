FROM java

RUN mkdir /debs2017

WORKDIR /debs2017

# Layer it for easier deployment on Docker
COPY metadata /debs2017/metadata

ADD target/debs-parrotbenchmark-system-1.0-SNAPSHOT.jar /debs2017/

# DEBS SPECIFIC ENV_VARS

ENV RABBIT_MQ_HOST_NAME_KEY="rabbit"
ENV HOBBIT_SESSION_ID_KEY=exp1
ENV SYSTEM_URI_KEY="http://project-hobbit.eu/resources/debs2017/JDEBS2017"
ENV SYSTEM_PARAMETERS_MODEL_KEY="{}"
ENV HOBBIT_EXPERIMENT_URI_KEY="http://project-hobbit.eu/resources/debs2017/experiment1"

# Run app.py when the container launches
CMD ["java", "-cp", "debs-parrotbenchmark-system-1.0-SNAPSHOT.jar", "romromov.DebsParrotBenchmarkSystemRunner"]
