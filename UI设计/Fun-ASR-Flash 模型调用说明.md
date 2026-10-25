```curl
curl --location --request POST 'https://cn-beijing.maas.qianwenaiapi.com/api/v1/services/aigc/multimodal-generation/generation' \
     --header "Authorization: Bearer $DASHSCOPE_API_KEY" \
     --header "Content-Type: application/json" \
     --header "X-DashScope-SSE: enable" \
     --data '{
    "model": "fun-asr-flash-2026-06-15",
    "input": {
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "input_audio",
                        "input_audio": {
                            "data": "https://xxx.wav"
                        }
                    }
                ]
            }
        ]
    },
    "parameters": {
        "format": "wav",
        "sample_rate": "16000"
    }
}'
```

上述是关于Fun-ASR-Flash 模型curl的使用介绍，目前项目再输入框里面存在一个麦克风，主要就是使用这个AI 模型