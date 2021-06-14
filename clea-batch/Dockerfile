FROM centos:7 AS installer


# Install all the locales
RUN sed -i 's/^override_install_langs=.*$/override_install_langs=fr_FR/g' /etc/yum.conf \
    && yum -y update \
    && yum -y install java-11-openjdk \
    && yum -y install epel-release \
    && yum -y install python3 python3-pip \
    && yum -y install nc \
    && yum -y install unzip \
    && useradd -g root javaapp

ENV LC_ALL fr_FR.utf8
ENV LANG fr_FR.utf8
ENV PATH $PATH:~/.local/bin


WORKDIR /tmp
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" \
    && unzip awscliv2.zip \
    && ./aws/install -i /usr/local/aws-cli -b /usr/local/bin \
    && rm awscliv2.zip rm -rf aws


WORKDIR /home/javaapp

USER javaapp
# Integration WebHook
RUN pip3 install --user s3cmd

FROM installer
COPY ./target/clea*-exec.jar ./clea-batch.jar
COPY ./src/main/scripts/* ./
COPY ./src/main/docker/aws ./.aws

USER root
RUN chmod +x *.sh \
 && mkdir /logs \
 && touch /logs/clea-batch.log \
 && touch /logs/clea-batch.error.log \
 && chmod a+rw /logs/*.log

USER javaapp

CMD [ "./clea-batch.sh" ]
