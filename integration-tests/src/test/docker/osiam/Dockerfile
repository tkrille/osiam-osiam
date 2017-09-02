FROM java:8-jre

COPY osiam.war /osiam.war
RUN chmod +x /osiam.war
RUN /osiam.war initHome --osiam.home=/var/lib/osiam
#COPY osiam_${database.container}.yaml /var/lib/osiam/config/osiam.yaml
COPY osiam_postgres.yaml /var/lib/osiam/config/osiam.yaml

#EXPOSE ${osiam.port} 8000
#ENV SERVER_PORT=${osiam.port} JAVA_OPTS="${osiam.debug.opts}"
EXPOSE 8180 8000
ENV SERVER_PORT=8180

CMD /osiam.war --osiam.home=/var/lib/osiam
