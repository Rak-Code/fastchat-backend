#!/usr/bin/env python3
"""Simple test to verify Groq API connectivity"""

import requests
import json

# Your new Groq API key
GROQ_API_KEY = "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"
GROQ_BASE_URL = "https://api.groq.com/openai"

def test_groq_api():
    print("🔍 Testing Groq API connectivity...")
    
    # Test 1: List available models
    print("\n1️⃣ Testing API key validity...")
    try:
        response = requests.get(
            f"{GROQ_BASE_URL}/v1/models",
            headers={
                "Authorization": f"Bearer {GROQ_API_KEY}",
                "Content-Type": "application/json"
            }
        )
        
        if response.ok:
            print("✅ API key is valid!")
            models = response.json()
            print(f"   Found {len(models.get('data', []))} available models")
        else:
            print(f"❌ API key test failed: {response.status_code}")
            print(f"   Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ API key test exception: {e}")
        return False
    
    # Test 2: Simple chat completion
    print("\n2️⃣ Testing chat completion...")
    try:
        response = requests.post(
            f"{GROQ_BASE_URL}/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {GROQ_API_KEY}",
                "Content-Type": "application/json"
            },
            json={
                "model": "llama-3.3-70b-versatile",
                "messages": [
                    {"role": "user", "content": "Say 'API key works!' if you can see this message."}
                ],
                "temperature": 0.1,
                "max_tokens": 50
            }
        )
        
        if response.ok:
            result = response.json()
            content = result['choices'][0]['message']['content']
            print(f"✅ Chat test successful!")
            print(f"   Response: {content}")
            return True
        else:
            print(f"❌ Chat request failed: {response.status_code}")
            print(f"   Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Chat request exception: {e}")
        return False

if __name__ == "__main__":
    success = test_groq_api()
    if success:
        print("\n🎉 All tests passed! Your API key is working correctly.")
    else:
        print("\n💥 Tests failed. Please check your API key.")