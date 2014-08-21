## Shoutout Services


### Autoscaling Notes

Generate your private key x.509 cert, I saved mine in ~/.aws/signingCert

```
openssl genrsa 1024 > private_key.pem
yes "" | openssl req -new -x509 -nodes -sha1 -days 3650 -key private_key.pem -outform PEM > cert.pem
```

The following keys were added to my ~/.bash_profile for command line access

```
export JAVA_HOME=/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home
export AWS_AUTO_SCALING_HOME=/NEOSAVVY/sdk/autoscaling
export AWS_CREDENTIAL_FILE=/Users/aparrish/.aws/credentials
export EC2_CERT=~/.aws/signingCert/cert.pem
export EC2_PRIVATE_KEY=~/.aws/signingCert/signing_cert.pem
```

I added the following to my $PATH in ~/.bash_profile:

```$AWS_AUTO_SCALING_HOME/bin```

The following command is used to generate the Launch Configuration:

as-create-launch-config shoutout-launch-configuration --instance-id i-484fae6b -I AKIAIFVHJNAMIMPCY56Q -S qyDIdmLEnpFTXxmXZMVV+AYfRwwe0DuFqXHNnJPs


### DROP TABLE:

```
DROP TABLE BLOCKED_USERS; DROP TABLE CONTACTS; DROP TABLE DATABASECHANGELOG; DROP TABLE DATABASECHANGELOGLOCK; DROP TABLE GROUPS; DROP TABLE GROUP_ITEMS; DROP TABLE SESSIONS; DROP TABLE SHOUTOUTS; DROP TABLE USERS; DROP TABLE USER_NAME_RESTRICTIONS;
```