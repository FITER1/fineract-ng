# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#
FROM azul/zulu-openjdk:12
MAINTAINER Aleksandar Vidakovic <cheetah@monkeysintown.com>

ENV PATH /opt/fineract:$PATH

WORKDIR /opt/fineract

ADD fineract-provider-*.tgz /opt/

EXPOSE 8443

CMD ["java", "-Xmx512m", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=UTC", "-Djava.net.preferIPv4Stack=true", "--illegal-access=permit", "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED", "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", "-jar", "fineract-provider*.jar", "--spring.profiles.active=default,basicauth"]
