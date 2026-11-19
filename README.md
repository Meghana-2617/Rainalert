# ☔ Rain Alert

A Java web app that predicts whether it will rain in the next few hours, using live data from the **OpenWeatherMap 5-day / 3-hour Forecast API**.

Pick a place (Jayanagar, BTM Layout or Sarjapur in Bengaluru) or type any latitude and longitude, click **Check Rain**, and the app tells you whether to carry an umbrella.

---

## ✨ Features

- 🌧️ **Rain prediction** for the next 6, 12 or 24 hours
- 📍 **3 preset places** in Bengaluru, plus custom latitude / longitude input
- 🕒 **Forecast timeline** every 3 hours: weather, temperature, rain chance and weather code
- 🎨 **Animated UI**: the sky turns stormy with falling rain, or sunny, based on the result
- 🔒 **API key kept on the server**, never exposed in the browser
- ✅ **Input validation** and clear error messages (400, 401, 429, 5xx)
- 🎭 **Demo mode** that works without an API key or internet

---

## 🏗️ How it works

```
┌───────────┐  GET /api/rain?lat=..&lon=..   ┌──────────────────┐   GET + API key   ┌────────────────┐
│  Browser  │ ─────────────────────────────▶ │   Java server    │ ────────────────▶ │ OpenWeatherMap │
│ HTML/CSS/ │                                 │ (RainAlertServer)│                   │  Forecast API  │
│    JS     │ ◀───────────────────────────── │  rain logic      │ ◀──────────────── │                │
└───────────┘   JSON { willRain, list }       └──────────────────┘    JSON forecast  └────────────────┘
```

1. The user selects a place or enters latitude and longitude.
2. The browser sends a **GET** request to the app's own REST endpoint, `/api/rain`.
3. The Java server calls the OpenWeatherMap Forecast API with the API key.
4. It parses the JSON response and checks each forecast's weather condition code.
5. If **any code is below 700**, it returns `willRain: true`.
6. The frontend shows the result and the forecast timeline.

### Rain rule

| Code | Meaning | Counts as rain? |
|---|---|---|
| 2xx | Thunderstorm | ✅ |
| 3xx | Drizzle | ✅ |
| 5xx | Rain | ✅ |
| 6xx | Snow | ✅ |
| 7xx | Mist / fog / haze | ❌ |
| 800 | Clear sky | ❌ |
| 80x | Clouds | ❌ |

---

## 🛠️ Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 11+ · `java.net.http.HttpClient` · built-in `com.sun.net.httpserver.HttpServer` |
| JSON parsing | [org.json](https://github.com/stleary/JSON-java) |
| Frontend | HTML · CSS · JavaScript (`fetch`, `async`/`await`) |
| API | [OpenWeatherMap 5-day / 3-hour Forecast](https://openweathermap.org/forecast5) |

---

## 📁 Project structure

```
rain-alert/
├── RainAlertServer.java   # Backend: web server + OpenWeatherMap call + rain logic
├── RainAlert.html         # Frontend: UI (HTML + CSS + JavaScript)
├── run.bat                # Windows: compile and start with one click
├── lib/
│   └── json-20240303.jar  # org.json library
└── README.md
```

---

## 🚀 Run it locally

### 1. Get a free API key
Sign up at [openweathermap.org](https://openweathermap.org/api), verify your email, and copy your key from **My API keys**. New keys can take up to 2 hours to activate.

### 2. Set the API key as an environment variable

**Windows (Command Prompt):**
```bat
set OWM_API_KEY=your_api_key_here
```

**macOS / Linux:**
```bash
export OWM_API_KEY=your_api_key_here
```

### 3. Compile and run

**Windows:** double-click `run.bat`, or run:
```bat
javac -cp "lib\json-20240303.jar" -d out RainAlertServer.java
java -cp "out;lib\json-20240303.jar" RainAlertServer
```

**macOS / Linux:**
```bash
javac -cp "lib/json-20240303.jar" -d out RainAlertServer.java
java -cp "out:lib/json-20240303.jar" RainAlertServer
```

### 4. Open the app
Go to **http://localhost:8080**.

---

## 🔌 API endpoint

### `GET /api/rain`

| Parameter | Required | Description |
|---|---|---|
| `lat` | ✅ | Latitude (-90 to 90) |
| `lon` | ✅ | Longitude (-180 to 180) |
| `cnt` | ❌ | Number of 3-hour forecasts to check (default `4` = next 12 hours) |

**Example request**
```
GET /api/rain?lat=12.9308&lon=77.5838&cnt=4
```

**Example response**
```json
{
  "willRain": false,
  "message": "No rain expected today.",
  "lat": "12.9308",
  "lon": "77.5838",
  "list": [
    { "dt": 1795611000, "main": { "temp": 299.1 }, "pop": 0, "weather": [{ "id": 802, "description": "scattered clouds" }] }
  ]
}
```

**Error responses**

| Status | When |
|---|---|
| `400` | Missing or invalid latitude / longitude |
| `401` | Invalid or inactive API key |
| `429` | Too many requests to OpenWeatherMap |
| `500` | API key not configured |
| `502` | OpenWeatherMap could not be reached |

---

## 🔒 Security

- The API key is read from the `OWM_API_KEY` **environment variable**.
- The key is **never sent to the browser**. Only the Java server talks to OpenWeatherMap.
- **Never commit your API key** to GitHub.

---

## 🔮 Future improvements

- Use `pop` (probability of precipitation) for a more accurate prediction
- Send SMS or email alerts every morning
- Detect the user's location with GPS
- Cache results for a few minutes to save API calls
- Migrate to Spring Boot

---

## 🙏 Credits

Weather data provided by [OpenWeatherMap](https://openweathermap.org/).
