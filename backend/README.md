# SuperShopCart App - Java Backend (Spring Boot)

## 🧱 Initial Architecture

- **Backend**: Java Spring Boot
- **Frontend**: Kotlin Android app (to be implemented later)
- **Database**: PostgreSQL (or H2 for local testing)

## 📄 Entity: `GroceryItem.java`

```java
@Entity
public class GroceryItem {
    

    // Constructors, Getters and Setters
}
```

## 🧍 Entity: `Shopper.java`

```java
@Entity
@Table(name = "shoppers")
public class Shopper {
    

    // Constructors, Getters and Setters
}
```

## 🛒 Entity: `ShopCartList.java`

```java
@Entity
public class ShopCart {

   
    // Getters and Setters
}
```

## 📁 Repositories

```java

```

## ⚙️ application.properties

```properties

```

## ✅ Recommended JDK for VS Code

### JDK 17 (LTS)

- Compatible with Spring Boot 2.6+ and 3.x
- Supported until 2029
- Stable and widely adopted

### Suggested Builds

- Adoptium Temurin 17
- Amazon Corretto 17
- Zulu OpenJDK 17

### Setup in VS Code

1. Install Java Extension Pack
2. Set JDK via `Java: Configure Java Runtime`
3. Verify in terminal:

```bash
java -version
javac -version
```

Should return something like:

```bash
openjdk version "17.x.x"
```

## FIREBASE

🚀 Firebase onboarding initiated — setting up your free Firebase account to enable Google Sign-In, hosting, database, push notifications, etc.

✅ Step-by-Step: Create a Free Firebase Account
1️⃣ Go to Firebase Console
🔗 https://console.firebase.google.com/

2️⃣ Sign in with a Google Account
Use the Google account you want associated with the Firebase project

No credit card needed for basic plan (Spark)

3️⃣ Create a New Project
Click “Add project”

Enter your project name (e.g., SuperShopApp)

Click Continue

✅ Disable Google Analytics (optional unless needed)

4️⃣ Wait for Firebase to initialize
You’ll be taken to the Firebase project dashboard.

🧪 Enable Google Sign-In (for OAuth2)
5️⃣ Enable Google Sign-In
Go to:

Authentication → Sign-in Method → Click Google → Enable

Enter your Project Support Email

Save

This allows Firebase to accept Google account logins from Android/iOS clients.

🔐 6️⃣ Get Web Client ID (used in your backend)
Go to:

Project settings (⚙️ icon top-left) → General → Scroll to Your apps

If you haven’t added a web app yet:

Click </> Add App

Register app (name it e.g., web-client)

Skip hosting setup

Firebase will generate a Web Client ID

✅ Copy this Client ID — you’ll use it in your backend in:

```java
.setAudience(Collections.singletonList("YOUR_GOOGLE_CLIENT_ID"))

```

✅ Firebase Free Tier (Spark Plan)
Feature	Limit
Auth (Google Sign-In)	✅ Unlimited
Firestore/Realtime DB	50K reads/day
Hosting	1 GB
Cloud Functions	125K invocations/month
Cost	$0 (no credit card needed)

✅ Recap
Step	Description
1.	Visit firebase.google.com
2.	Sign in and create a project
3.	Enable Authentication > Google Sign-In
4.	Copy your Web Client ID
5.	Use it in your Spring Boot backend to verify Google tokens
6.	Start building 🎉

Password validation	✅ via BCryptPasswordEncoder.matches()

🔐 Full AuthenticationController for Firestore + JWT + Google Login + Refresh Tokens
Let’s put it all together: secure, stateless, Firestore-native, and production-ready.

✅ Features Included:

Endpoint	Purpose
POST /auth/register	Register shopper with email/password
POST /auth/login	Login with email/password
POST /auth/google	Login via Google Sign-In token
POST /auth/refresh	Issue new access token from refresh
POST /auth/logout	Revoke refresh token

✅ Firestore Collection Structure
Collection: refresh_tokens

```json
Document ID	shopperId	expiry (timestamp)
<refreshToken>	"abc123"	1724000123456
```

In the context of JWT (JSON Web Token), Claims are the key-value pairs (data) embedded inside the token's payload. They represent information about the user, session, or context.

🧱 JWT Structure (Visual Breakdown)

A JWT looks like this:

```php
<Header>.<Payload>.<Signature>
```

The Payload contains the Claims — here’s an example decoded:

```json
{
"sub": "abc123",
"role": "USER",
"deviceId": "android-xyz",
"iat": 1724000000,
"exp": 1724003600
}

```

✅ What Are Claims?
🔐 Claims = pieces of information about the authenticated user/session that the backend encodes inside the token.

They are signed, so they cannot be tampered with without invalidating the signature.

🔸 Common Types of Claims
1. Standard Claims (Defined by the JWT spec)

| Claim | Meaning                     |
|-------|-----------------------------|
| sub   | Subject (e.g. user ID)      |
| iat   | Issued at timestamp         |
| exp   | Expiration timestamp        |
| iss   | Issuer                      |
| aud   | Audience                    |

These help enforce identity and token lifespan.

2. Custom Claims (You define them)

|Claim	      | Example	        | Purpose                      |
|------------| -----------------  | ---------------------------- |
| role	      | "ADMIN" or "USER"	| Used for access control      |
| deviceId	  | "android-xyz-123"	| Binds token to a device      |
| email	     | "mario@gmail.com"	| Lightweight identity info    |
| locale	    | "pt-BR"	        | UI customization per user    |


✅ In Java (JJWT Library)
In code, extract claims like this:

```java
Claims claims = Jwts.parserBuilder()
.setSigningKey(secretKey)
.build()
.parseClaimsJws(token)
.getBody();

String userId = claims.getSubject(); // "sub"
String deviceId = claims.get("deviceId", String.class);
String role = claims.get("role", String.class);
```

🛡️ Security Implications
✅ Claims are signed, so the server can trust them

❌ Claims are not encrypted, so anyone can read the payload if they decode the JWT

🔐 Sensitive data like passwords or tokens should never go in claims

Add /auth/logout-all: log out from all devices for a shopper
Rename JwtUtil to JwtTokenService for better clarity
It handles JWT creation and parsing — it's a service, not just a utility.

Scheduled cleanup script that deletes expired refresh tokens nightly from Firestore:
- run automatically (e.g., nightly at 2 AM)
- remove tokens from refresh_tokens collection where expiry < now
- keeps Firestore lean and secure


## 🔍 Your Components

| Component	| Purpose	| Required?  |
| -------- | -------- |------------|
| GoogleTokenVerifier |Verifies Google ID tokens (from client-side OAuth login) | 	✅ Yes |
| JwtAuthFilter	 | Extracts and validates your own JWT, attaches user to SecurityContext| ✅ Yes |
| FirebaseConfig	| Configures Firestore client (for emulator or production)	 | ✅ Yes |

### ✅ Is this setup valid?
Yes, and you're following a secure, scalable pattern:

1. Frontend (e.g., React/JS):
    Uses firebase.auth().signInWithPopup() to authenticate via Google
2. Frontend sends:
   The id_token (from Google) to your backend
3. Backend:
   Uses GoogleTokenVerifier to validate the token (and user’s email/client ID)
4. Backend (optional):
   Issues a custom JWT signed with your backend's secret — used for stateless session control
5. Every request after that:
   Goes through JwtAuthFilter to validate your own JWT and extract user info

### ✅ Do you need both GoogleTokenVerifier and JwtAuthFilter?
Yes — if you're issuing your own JWTs after verifying Google's token (which is a best practice).

## 📌 Why this two-step flow?
GoogleTokenVerifier is used only once during login to verify the Google ID token

### You then issue your own JWT that:

- Has your own claims (e.g., shopper ID)
- Has your own expiration time
- Is shorter-lived, scoped, and decoupled from Google’s token format

This gives you full control over authentication and simplifies token checking in JwtAuthFilter.

## Solution for sharing ShopCart lists between shoppers:ShopCart Sharing Implementation

A permission-based sharing system that's both secure and user-friendly. Here's how it works:

### Key Features:

1. Email-based Sharing: Shopper1 shares by entering Shopper2's email 
    address (the same Google account email they use to sign in)
2. Permission Levels:

- READ_ONLY: Can view the cart
- EDIT: Can add/remove items
- ADMIN: Can share with others and manage the cart

3. Security: Only cart creators and users with ADMIN permissions can share carts with others

## Mobile App Flow:

### For Shopper1 (sharing):

1. Open their cart in the app
2. Tap "Share Cart" button
3. Enter Shopper2's email address
4. Select permission level (READ_ONLY, EDIT, or ADMIN)
5. Send share request to backend

### For Shopper2 (receiving):

1. The shared cart automatically appears in their cart list the next time they refresh/open the app
2. They can interact with it based on the permission level granted

## Backend API Endpoints:

- POST /api/carts/{cartId}/share - Share a cart with another user
- DELETE /api/carts/{cartId}/share/{targetShopperId} - Remove sharing

## Why This Solution is Best:

Leverages Existing Architecture: Uses your current Google OAuth + Firestore setup
Real-time Sync: Firestore automatically syncs changes across devices
Secure: Permission-based access control
Scalable: Can easily extend to support multiple shared users per cart
User-friendly: Simple email-based sharing that users are familiar with

Mobile Implementation Notes:

The Android app should periodically refresh the cart list to show newly shared carts
Consider implementing push notifications when a cart is shared
The shopperIds field in your existing ShopCart model already supports multiple users, so this solution builds on your current design

This approach provides a seamless sharing experience while maintaining security and leveraging your existing Google OAuth authentication system.


Improved Business Rules Summary:
✅ Better State Management

SHOPPING: Intermediate state when items are purchased but cart still active
COMPLETED: Manual completion by shoppers (not automatic)
TEMPLATE: Reusable carts for recurring shopping lists
ARCHIVED: Long-term storage (6 months) instead of deletion

✅ Permission-Aware Operations

Edit/delete permissions based on sharing roles
Creator and ADMIN can delete, EDIT+ can modify
No more blanket restrictions on active carts

✅ Shared Shopping Friendly

Manual completion prevents premature state changes
Easy restart of shopping sessions from completed carts
Template system for recurring lists ("Weekly Groceries")
Preserves sharing relationships long-term

✅ Realistic Timelines

6-month archiving instead of 30-day deletion
Archived carts maintain shopper associations
Templates persist indefinitely

✅ Better User Experience

Shoppers control when shopping is "done"
Easy reuse of completed shopping lists
Templates for frequently used lists
Clear permission boundaries

This approach is much more suitable for shared shopping because it:

Preserves relationships between shoppers
Gives users control over cart lifecycle
Supports recurring shopping patterns
Respects sharing permissions
Matches real-world usage patterns


AuthController's implementation of the Option 1: "Continue with Google" flow (i.e., POST /auth/google) and assess if it's well implemented or needs any improvements.

✅ GOAL OF THIS FLOW
For the endpoint:

http
Copiar
Editar
POST /auth/google
It should:

Extract the Google ID token from the Authorization header.

Verify the token using a GoogleTokenVerifier.

Extract user info (email and name) from the token.

Check if the user exists in the database:

✅ If yes, login.

✅ If not, register a new user (with a random password or placeholder).

Issue JWT access and refresh tokens.

Store the refresh token per device in Firestore.

✅ YOUR CURRENT IMPLEMENTATION ANALYSIS
🔹 Step-by-step Review
✅ Step 1: Extract & validate Authorization header
java
Copiar
Editar
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
}
✅ Correct. Verifies proper format of the Bearer token.

✅ Step 2: Extract token & verify with Google
java
Copiar
Editar
String idToken = authHeader.substring(7).trim();
GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);

if (payload == null) {
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google token");
}
✅ Correct. Extracts and verifies the ID token using the GoogleTokenVerifier.

✅ Step 3: Extract email and name
java
Copiar
Editar
String email = payload.getEmail();
String name = (String) payload.get("name");
✅ Good. These are the most relevant claims from the payload.

✅ Step 4: Shopper lookup or registration
java
Copiar
Editar
Shopper shopper = shopperRepository.findByEmail(email)
.orElseGet(() -> {
Shopper s = new Shopper(email, name, UUID.randomUUID().toString());
try {
return shopperRepository.save(s);
} catch (Exception e) {
throw new RuntimeException("Could not save shopper", e);
}
});
✅ Good fallback registration logic for first-time users.

🟡 Note: Password is set to a random UUID string. That’s acceptable since users won't log in with it — but you may want to clearly distinguish Google users in the database, or store "google" in a provider field.

✅ Step 5: Generate access and refresh tokens
java
Copiar
Editar
String accessToken = jwtTokenService.generateAccessToken(shopper.getId(), deviceId);
String refreshToken = jwtTokenService.generateRefreshToken(shopper.getId());
✅ Correct use of service methods.

✅ Step 6: Store refresh token per device
java
Copiar
Editar
RefreshToken tokenRecord = new RefreshToken(
refreshToken,
shopper.getId(),
deviceId,
System.currentTimeMillis() + jwtTokenService.getRefreshTokenExpiration()
);

firestore.collection("refresh_tokens").document(refreshToken).set(tokenRecord).get();
✅ Correctly stores the refresh token in Firestore and blocks until write is done.

✅ Step 7: Return tokens
java
Copiar
Editar
return ResponseEntity.ok(Map.of(
"accessToken", accessToken,
"refreshToken", refreshToken
));
✅ Well-formed response.

