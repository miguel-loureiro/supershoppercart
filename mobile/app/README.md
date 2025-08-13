### Super Shopper Cart Android App

## 🔹  Flow Summary

[Mobile App] Google Sign-In → ID Token
↓
[Backend] GoogleTokenVerifier verifies token
↓
[Backend] JwtTokenService issues JWT Access & Refresh tokens
↓
[Mobile App] Stores JWT tokens in TokenManager
↓
[Mobile App] Uses JWT Access Token for future API calls

## This is the correct, secure Google Sign-In + backend JWT pattern.

Yes ✅ — that endpoint matches perfectly with the Google Sign-In flow we’ve designed for the Android app.

## Here’s why it’s correct:

1. Receives Google ID Token securely
   You’re taking the ID token from the Authorization: Bearer <idToken> header.

That token is generated in the mobile app by Google Play Services’ GoogleSignIn API.

You’re not relying on any client-generated credentials for authentication — good.

2. Verifies it properly

   ```java

   GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);
   // Uses Google’s official verifier.
```
Confirms:

Token signature matches Google’s public keys.

Token audience matches your oauth.id (Web Client ID).

Token has not expired.

Rejects invalid tokens with 401 Unauthorized.

3. User account handling
   Checks if the shopper exists in Firestore (shopperRepository.findByEmail(email)).

Creates one if it doesn’t exist.

### This means first-time Google users are automatically onboarded — smooth UX.

4. Issues backend JWTs

   ```java

   String accessToken = jwtTokenService.generateAccessToken(shopper.getId(), deviceId);
   String refreshToken = jwtTokenService.generateRefreshToken(shopper.getId());
   Uses your own JwtTokenService.
````

accessToken → short-lived for API calls.
refreshToken → long-lived for token refresh.

5. Device-based refresh token management
   Saves the refresh token to refresh_tokens collection in Firestore with:

Shopper ID

Device ID

Expiration

This allows you to:

Invalidate tokens for a specific device.
Support multiple logged-in devices for the same account.

6. Returns exactly what the app expects

 ```json
 
   {
   "accessToken": "...",
   "refreshToken": "..."
   }
   
```

Matches the AuthModels data class in your mobile code.

The LoginViewModel can directly store these in TokenManager.

7. Security Best Practices you already follow
   Verifies Google token server-side — avoids trusting the client.

Issues your own JWTs — you don’t rely on Google tokens for internal API auth.
Stores refresh tokens in DB — allows revocation and tracking.
Requires Device ID — supports per-device session control.
Retrofit is a popular type-safe HTTP client for Android and Java.
It simplifies the process of making network requests by turning a REST API into a clean, well-defined interface.
Essentially, you define the structure of your API calls using annotations, and Retrofit handles the heavy lifting of making the actual network requests.

## Key Features and How It Works
- Declarative API Interface:
    - You define your API endpoints and their HTTP methods (like GET, POST, PUT, DELETE) in a Kotlin interface. Annotations like @GET, @POST, and @Path are used to specify the endpoint URL and parameters.
    - This makes your network code easy to read and maintain.
- Data Serialization:
    - Retrofit works with a serialization library (like Gson, Moshi, or Jackson) to automatically convert JSON or other data formats from the API response into Kotlin objects.
    - You just need to define your data classes, and Retrofit takes care of the rest.
- Concurrency:
    - Retrofit handles network requests on a background thread by default, which is crucial for not blocking the main UI thread and causing the app to become unresponsive.
    - It integrates seamlessly with Kotlin coroutines, which provides a modern and simple way to manage asynchronous operations.
- Integration with Coroutines:
    - With the suspend keyword, you can make Retrofit calls directly from a coroutine, making the code look and feel synchronous.
    - This eliminates the need for callbacks and makes the code cleaner and more readable.

## A Simple Example in Kotlin
Here's a quick look at the core components:

1. Define the API Service Interface

## Define a data class to represent the response

```kotlin
data class Post(
val userId: Int,
val id: Int,
val title: String,
val body: String
)
```

## Define the Retrofit interface with annotations

```kotlin
interface MyApiService {
@GET("posts/{id}")
suspend fun getPost(@Path("id") postId: Int): Post
}
```

2. Create a Retrofit Instance

```Kotlin

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val retrofit = Retrofit.Builder()
.baseUrl("https://jsonplaceholder.typicode.com/")
.addConverterFactory(GsonConverterFactory.create())
.build()

val apiService = retrofit.create(MyApiService::class.java)
```

3. Make the API Call

```Kotlin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

runBlocking {
launch(Dispatchers.IO) {
try {
val post = apiService.getPost(1)
println("Post Title: ${post.title}")
} catch (e: Exception) {
println("Error: ${e.message}")
}
}
}
```

## Integrate deviceId handling into LoginScreen so the flow is automatic:

### 1. On first login attempt, we’ll check if a deviceId exists in TokenManager.
If not, generate a UUID, store it, and use it for the backend call.
From then on, the same deviceId is reused.

### 2. What happens now
User taps Sign in with Google.

If deviceId doesn’t exist in DataStore, we generate and store one.

We call your /google backend with:

```json
Authorization: Bearer <idToken>
X-Device-Id: <deviceId>
```

- Backend verifies ID token, creates or finds the user, generates JWT tokens.
- Tokens are saved in TokenManager.
- User is redirected to the WelcomeScreen.

### MainActivity must navigate to LoginScreen if TokenManager.isLoggedIn() is false,instead of showing WelcomeScreen immediately

## Google Sign-in

Using the latest Google Sign-In flow per the Android docs on Credential Manager + Sign in with Google (SIWG),
instead of the legacy GoogleSignInClient.
Please see: https://developer.android.com/identity/sign-in/credential-manager-siwg

### Creating a ´´nonce´´
Please see: https://developer.android.com/google/play/integrity/classic#nonce