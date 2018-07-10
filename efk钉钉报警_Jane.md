# 环境：

centos7

Fluent-bit 0.13

Elasticsearch 6.3.0

Kibana 6.3.0

Python 2.7.5



# 安装：

## 安装elastalert

```
git clone https://github.com/Yelp/elastalert.git 
cd elastalert
python setup.py install 
Pip install -r requirements.txt   
cp config.yaml.example config.yaml
```

以上步骤会生成elastalert文件夹

## 安装elastalert的钉钉插件

`git clone https://github.com/xuyaoqiang/elastalert-dingtalk-plugin` 

把elastalert-dingtalk-plugin中的elastalert_modules、rules和config.yaml复制到上一步的elastalert下

(以上命令执行时候可能会遇到错误,解决过程未记录)



# 运行：

以钉钉报警为例

## 运行fluent-bit, Elasticsearch和kibana 

其中elasticsearch运行地址：106.75.229.247 监听端口：9200

Fluent-bit 配置文件（以获取cpu信息为例）

## 运行elastalert

Elastalert报警分为三(以下报警说明中的路径均在elastalert文件夹下)： 

### 配置./config.yaml

主要配置内容：es_host, es_port

### 配置example_rules/example_test.yaml

此处报警规则自定义

```
Alert:

   -“debug”

   -"elastalert_modules.dingtalk_alert_modified.DingTalkAlerter"#此为钉钉报警插件实现的类

dingtalk_webhook #钉钉url

dingtalk_msgtype: text #报警文本类型

```

### 自定义报警

修改elastalert_modules/ding_alert.py文件

主要修改DingTalkAlerter.alert方法中的`body=create_alert_body()`这一句

### 运行命令

`python -m elastalert.elastalert --verbose --rule example_rules/example_test.yaml` 

即可通过钉钉报警



# 结果示例：

![](C:\Users\86152\Desktop\res.png)



# 配置文件说明：

./config.yaml, example_rules/example_test.yaml文件说明

## config.yaml中的配置项

- Rules_folder：用来加载下一阶段rule的设置，默认是example_rules
- Run_every：用来设置定时向elasticsearch发送请求
- Buffer_time：用来设置请求里时间字段的范围，默认是45分钟
- Es_host：elasticsearch的host地址
- Es_port：elasticsearch 对应的端口号
- Use_ssl：可选的，选择是否用SSL连接es，true或者false
- Verify_certs：可选的，是否验证TLS证书，设置为true或者false，默认为- true
- Es_username：es认证的username
- Es_password：es认证的password
- Es_url_prefix：可选的，es的url前缀（我的理解是https或者http）
- Es_send_get_body_as：可选的，查询es的方式，默认的是GET
- Writeback_index：elastalert产生的日志在elasticsearch中的创建的索引
- Alert_time_limit：失败重试的时间限制

## Elastalert中rule的规则：

- name：配置，每个rule需要有自己独立的name，一旦重复，进程将无法启动。

- type：配置，选择某一种数据验证方式。 

- index：配置，从某类索引里读取数据，目前已经支持Ymd格式，需要先设置 use_strftime_index:true，然后匹配索引，配置形如：index: logstash-es-test%Y.%m.%d，表示匹配logstash-es-test名称开头，以年月日作为索引后缀的index。

- filter：配置，设置向ES请求的过滤条件。

- timeframe：配置，累积触发报警的时长。

- alert：配置，设置触发报警时执行哪些报警手段。不同的type还有自己独特的配置选项。目前ElastAlert 有以下几种自带ruletype：

  ​     any：只要有匹配就报警；

  ​     blacklist：compare_key字段的内容匹配上 blacklist数组里任意内容；

  ​     whitelist：compare_key字段的内容一个都没能匹配上whitelist数组里内容；

  ​     change：在相同query_key条件下，compare_key字段的内容，在 timeframe范围内 发送变化；

  ​     frequency：在相同 query_key条件下，timeframe 范围内有num_events个被过滤出 来的异常；

  ​     spike：在相同query_key条件下，前后两个timeframe范围内数据量相差比例超过spike_height。其        中可以通过spike_type设置具体涨跌方向是- up,down,both 。还可以通过threshold_ref设置要求上一个周期数据量的下限，threshold_cur设置要求当前周期数据量的下限，如果数据量不到下限，也不触发；

  ​     flatline：timeframe 范围内，数据量小于threshold 阈值；

  ​     new_term：fields字段新出现之前terms_window_size(默认30天)范围内最多的terms_size (默认50)个结果以外的数据；

  ​     cardinality：在相同 query_key条件下，timeframe范围内cardinality_field的值超过 max_cardinality 或者低于min_cardinality



# 示例文件

Fluent-bit, ./config.yml , elastalert/example_frequency配置文件内容如下： 

## Fluent-bit配置如下：

读取系统cpu信息存储到elasticsearch中 

```
[SERVICE]
  Flush         1
  Log_Level     info
  Daemon        off
  Parsers_File  parsers.conf

[INPUT]
  Name  cpu
  Tag my_cpu
[OUTPUT]
  Name            es
  Match           *
  Host            ${FLUENT_ELASTICSEARCH_HOST}
  Port            ${FLUENT_ELASTICSEARCH_PORT}
  Logstash_Format On
  Retry_Limit     False

```

## ./config.yml文件

主要配置es_host,es_port, (writeback_index为将报警信息存储到数据库中的索引名称，可选) 

```
# This is the folder that contains the rule yaml files
# Any .yaml file will be loaded as a rule
rules_folder: example_rules

# How often ElastAlert will query Elasticsearch
# The unit can be anything from weeks to seconds
run_every:
  minutes: 5

# ElastAlert will buffer results from the most recent
# period of time, in case some log sources are not in real time
buffer_time:
  minutes: 5

# The Elasticsearch hostname for metadata writeback
# Note that every rule can have its own Elasticsearch host
es_host: 106.75.229.247

# The Elasticsearch port
es_port: 9200

#smtp_host: smtp.163.com
#smtp_port: 25
#smtp_auth_file: smtp_auth_file.yaml
#from_addr: '发送者@163.com'
#email_reply_to: '接收者@qq.com'
#use_ssl: False

# The AWS region to use. Set this when using AWS-managed elasticsearch
#aws_region: us-east-1

# The AWS profile to use. Use this if you are using an aws-cli profile.
# See http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html
# for details
#profile: test

# Optional URL prefix for Elasticsearch
#es_url_prefix: elasticsearch

# Connect with TLS to Elasticsearch
#use_ssl: True

# Verify TLS certificates
#verify_certs: True

# GET request with body is the default option for Elasticsearch.
# If it fails for some reason, you can pass 'GET', 'POST' or 'source'.
# See http://elasticsearch-py.readthedocs.io/en/master/connection.html?highlight=send_get_body_as#transport
# for details
#es_send_get_body_as: GET

# Option basic-auth username and password for Elasticsearch
#es_username: someusername
#es_password: somepassword

# Use SSL authentication with client certificates client_cert must be
# a pem file containing both cert and key for client
#verify_certs: True
#ca_certs: /path/to/cacert.pem
#client_cert: /path/to/client_cert.pem
#client_key: /path/to/client_key.key

# The index on es_host which is used for metadata storage
# This can be a unmapped index, but it is recommended that you run
# elastalert-create-index to set a mapping
writeback_index: elastalert_status

# If an alert fails for some reason, ElastAlert will retry
# sending the alert until this time period has elapsed
alert_time_limit:
  days: 2

```

## example_rules/example_test.yaml文件

主要配置报警规则（以elasticsearch数据库中的logstash-*索引下cpu0_p_user值为0在20分钟内超过两次发送警告为例） 

配置alert，dingtalk_webhook，dingtalk_msgtype。

```
#Alert when the rate of events exceeds a threshold

# (Optional)
# Elasticsearch host
#es_host: 106.75.229.247

# (Optional)
# Elasticsearch port
#es_port: 9200

# (OptionaL) Connect with SSL to Elasticsearch
#use_ssl: True

# (Optional) basic-auth username and password for Elasticsearch
#es_username: someusername
#es_password: somepassword

# (Required)
# Rule name, must be unique
name: zxl_test_rule

# (Required)
# Type of alert.
# the frequency rule type alerts when num_events events occur with timeframe time
type: frequency

# (Required)
# Index to search, wildcard supported
index: logstash-*

# (Required, frequency specific)
# Alert when this many documents matching the query occur within a timeframe
num_events: 2

# (Required, frequency specific)
# num_events must occur within this amount of time to trigger an alert
timeframe:
  minutes: 20

# (Required)
# A list of Elasticsearch filters used for find events
# These filters are joined with AND and nested in a filtered query
# For more info: http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl.html
filter:
- query_string:
    query: "cpu0_p_user: 0"

# (Required)
# The alert is use when a match is found
alert:
#- "email" 
- "debug" #钉钉方式告警
- "elastalert_modules.dingtalk_alert_modified.DingTalkAlerter"#此为钉钉报警插件实现的类

dingtalk_webhook: 'https://oapi.dingtalk.com/robot/send?access_token=cda32502388da442f4bfed3bd4b80346c786c272362b0dcdb74a9208ca745c45'
dingtalk_msgtype: text
#- "debug"
#- "command"
#pipe_match_json: true
#command: ["/home/jane/alertfile/php_alert.php"]
# (required, email specific)
# a list of email addresses to send alerts to
#email:
#- 接收者@XX.com

```

## 自定义报警格式

可以根据需要改写`elastalert_modules/ding_talk_alert.py`文件中`DingTalkAlerter`类的`alert`方法中`body=create_alert_body(matches)`这一句，其中`create_alert_body（）`函数返回`unicode`字符串，`matches`是字典类型。

这里根据目前elasticsearch中字段类型，新增了方法：

```
def my_create_alert_body(self,matches):
        body='WARNING HOST:'+self.es_host+'\n'
        for match in matches:
            body+='CPU_P: '+str(match['cpu_p'])+'\n'
            body+='USER_P: '+str(match['user_p'])+'\n'
            if len(matches)>1:
                body+='\n'
        return unicode(body)

```

该方法从`match`中获取`cpu_p`和`user_p`两个字段，并且将结果转为`unicode`类型返回。

并且在alert方法中使用`my_create_alert_body(matches)`方法来构建报警体。



# elastalert目录结构

```
└── elastalert
    ├── build
    │   ├── bdist.linux-x86_64
    │   └── lib
    │       ├── elastalert
    ├── changelog.md
    ├── config.yaml #elastalert报警插件的配置文件（需配置）
    ├── config.yaml.example
    ├── dist
    │   └── elastalert-0.1.32-py2.7.egg
    ├── docker-compose.yml
    ├── Dockerfile-test
    ├── docs
    │   ├── Makefile
    │   └── source
    │       ├── conf.py
    │       ├── elastalert.rst
    │       ├── elastalert_status.rst
    │       ├── index.rst
    │       ├── recipes
    │       │   ├── adding_alerts.rst
    │       │   ├── adding_enhancements.rst
    │       │   ├── adding_rules.rst
    │       │   ├── signing_requests.rst
    │       │   └── writing_filters.rst
    │       ├── ruletypes.rst
    │       ├── running_elastalert.rst
    │       └── _static
    ├── elastalert
    │   ├── alerts.py 主要报警方法实现类
    │   ├── ……
    ├── elastalert.egg-info
    ├── elastalert_modules
    │   ├── dingtalk_alert_modified.py 
    │   ├── dingtalk_alert_modified.pyc
    │   ├── dingtalk_alert.py #主要钉钉报警方法实现类（可改写）
    │   ├── dingtalk_alert.pyc
    │   ├── __init__.py
    │   └── __init__.pyc
    ├── example_rules 报警规则配置文件（需配置）
    │   ├── api_error.yaml
    │   ├── avg_request_time.yaml
    │   ├── example_cardinality.yaml
    │   ├── example_change.yaml
    │   ├── example_frequency.yaml
    │   ├── example_new_term.yaml
    │   ├── example_opsgenie_frequency.yaml
    │   ├── example_percentage_match.yaml
    │   ├── example_single_metric_agg.yaml
    │   ├── example_spike.yaml
    │   ├── example_test.yaml 此为测试的报警规则
    │   ├── __init__.py
    │   ├── jira_acct.txt
    │   └── smtp_auth_file.yaml 邮箱认证的配置文件（邮件报警时需配置）
    ├── LICENSE
    ├── Makefile
    ├── README.md
    ├── requirements-dev.txt
    ├── requirements.txt
    ├── rules #此为钉钉报警插件的配置文件
    │   ├── api_error.yaml
    │   └── avg_request_time.yaml
    ├── setup.cfg
    ├── setup.py
    ├── supervisord.conf.example
    ├── tests
    └── tox.ini

```

