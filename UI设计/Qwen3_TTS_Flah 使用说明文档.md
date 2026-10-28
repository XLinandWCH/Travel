```python
# coding=utf-8

import dashscope
from dashscope.audio.tts_v2 import *

# If the API Key is not configured in the environment variable, your-api-key needs to be replaced with your own API Key
# dashscope.api_key = "your-api-key"

dashscope.base_websocket_api_url='wss://cn-beijing.maas.qianwenaiapi.com/api-ws/v1/inference'

model = "qwen-audio-3.0-tts-flash"

voice = "longanfengyue"

synthesizer = SpeechSynthesizer(model=model, voice=voice)
audio = synthesizer.call("How is the weather today?")

with open('output.mp3', 'wb') as f:
    f.write(audio)
```

使用说明在我们的MySQL数据库中，
mysql> show tables;
+--------------------+
| Tables_in_travel   |
+--------------------+
| broadcast_messages |
| feedback_messages  |
| feedback_tickets   |
| llm_models         |
| model_api_keys     |
| users              |
| voice_models       |
+--------------------+
7 rows in set (0.00 sec)
有一个voice_models  的表
mysql> DESC voice_models;
+------------+--------------+------+-----+---------+----------------+
| Field      | Type         | Null | Key | Default | Extra          |
+------------+--------------+------+-----+---------+----------------+
| id         | bigint       | NO   | PRI | NULL    | auto_increment |
| model_id   | varchar(64)  | NO   | UNI | NULL    |                |
| api_key    | varchar(256) | NO   |     | NULL    |                |
| url        | varchar(256) | NO   |     | NULL    |                |
| model_type | varchar(8)   | NO   |     | NULL    |                |
| created_at | datetime(6)  | NO   |     | NULL    |                |
+------------+--------------+------+-----+---------+----------------+
6 rows in set (0.00 sec)

mysql> 
里面目前有一个ASR模型，里面的api_key 和我们目前这个qwen3-tts-flash 的tts模型的api_key 和ASR 是共用的，所以建表的时候，你直接复制就好了，如果你不知道是什么你也可以直接问我
对于的音频图片有3张：
开始语音图片：@app/shared/src/commonMain/composeResources/drawable/start_reading.xml
播放语音图片：@app/shared/src/commonMain/composeResources/drawable/reading.xml
禁止/关闭语音图片：@app/shared/src/commonMain/composeResources/drawable/close_reading.xml

要求：在AI生成完内容后底下，在
@app/shared/src/commonMain/composeResources/drawable/regenerate.xml
中间加上@app/shared/src/commonMain/composeResources/drawable/start_reading.xml 这个图标
@app/shared/src/commonMain/composeResources/drawable/copy.xml

如果用户点击了@app/shared/src/commonMain/composeResources/drawable/reading.xml 这个图标就变成@app/shared/src/commonMain/composeResources/drawable/reading.xml 这个图标
如果朗读完成，就变回@app/shared/src/commonMain/composeResources/drawable/reading.xml  这个图标。
在聊天窗口的顶部有一个@app/shared/src/commonMain/kotlin/com/xlin/travel/chat_screen/MainChatContent.kt 这个，它在左边
在@app/shared/src/commonMain/kotlin/com/xlin/travel/chat_screen/MainChatContent.kt 它的最右侧（也就是row分出左右两边，我记得有专门的组件就是干这个的），放@app/shared/src/commonMain/composeResources/drawable/close_reading.xml 这个图标
默认情况下是@app/shared/src/commonMain/composeResources/drawable/close_reading.xml 这个图标，如果用户点击之后就变成这个图标：@app/shared/src/commonMain/composeResources/drawable/start_reading.xml
这意味着模型自动朗读AI生成的最新内容（不包括思考内容），如果是close_reading.xml就不自动阅读
希望你了解这两个位置的区别。
一个是用户自己让模型读内容，一个是模型自己读内容