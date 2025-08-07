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


## Improved Business Rules Summary:
### ✅ Better State Management
- SHOPPING: Intermediate state when items are purchased but cart still active
- COMPLETED: Manual completion by shoppers (not automatic)
- TEMPLATE: Reusable carts for recurring shopping lists
- ARCHIVED: Long-term storage (6 months) instead of deletion

### ✅ Permission-Aware Operations
- Edit/delete permissions based on sharing roles
- Creator and ADMIN can delete, EDIT+ can modify
- No more blanket restrictions on active carts

### ✅ Shared Shopping Friendly
- Manual completion prevents premature state changes
- Easy restart of shopping sessions from completed carts
- Template system for recurring lists ("Weekly Groceries")
- Preserves sharing relationships long-term

### ✅ Realistic Timelines
- 6-month archiving instead of 30-day deletion
- Archived carts maintain shopper associations
- Templates persist indefinitely

### ✅ Better User Experience
- Shoppers control when shopping is "done"
- Easy reuse of completed shopping lists
- Templates for frequently used lists
- Clear permission boundaries

### This approach is much more suitable for shared shopping because it:
- Preserves relationships between shoppers
- Gives users control over cart lifecycle
- Supports recurring shopping patterns
- Respects sharing permissions
- Matches real-world usage patterns

## AuthController's implementation of the Option 1: "Continue with Google" flow (i.e., POST /auth/google) and assess if it's well implemented or needs any improvements.

✅ GOAL OF THIS FLOW
For the endpoint:

```http
POST /auth/google
```
### It should:
- Extract the Google ID token from the Authorization header.
- Verify the token using a GoogleTokenVerifier.
- Extract user info (email and name) from the token.
- Check if the user exists in the database:

✅ If yes, login.

✅ If not, register a new user (with a random password or placeholder).

Issue JWT access and refresh tokens.

Store the refresh token per device in Firestore.

✅ YOUR CURRENT IMPLEMENTATION ANALYSIS
🔹 Step-by-step Review
✅ Step 1: Extract & validate Authorization header

```java
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
}
```
✅ Correct. Verifies proper format of the Bearer token.

✅ Step 2: Extract token & verify with Google
```java
String idToken = authHeader.substring(7).trim();
GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);

if (payload == null) {
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google token");
}
```
✅ Correct. Extracts and verifies the ID token using the GoogleTokenVerifier.

✅ Step 3: Extract email and name
```java
String email = payload.getEmail();
String name = (String) payload.get("name");
```
✅ Good. These are the most relevant claims from the payload.

✅ Step 4: Shopper lookup or registration
```java
Shopper shopper = shopperRepository.findByEmail(email)
.orElseGet(() -> {
Shopper s = new Shopper(email, name, UUID.randomUUID().toString());
try {
return shopperRepository.save(s);
} catch (Exception e) {
throw new RuntimeException("Could not save shopper", e);
}
});
```
✅ Good fallback registration logic for first-time users.

🟡 Note: Password is set to a random UUID string. That’s acceptable since users won't log in with it — but you may want to clearly distinguish Google users in the database, or store "google" in a provider field.

✅ Step 5: Generate access and refresh tokens
```java
String accessToken = jwtTokenService.generateAccessToken(shopper.getId(), deviceId);
String refreshToken = jwtTokenService.generateRefreshToken(shopper.getId());
```
✅ Correct use of service methods.

✅ Step 6: Store refresh token per device
```java
RefreshToken tokenRecord = new RefreshToken(
refreshToken,
shopper.getId(),
deviceId,
System.currentTimeMillis() + jwtTokenService.getRefreshTokenExpiration()
);

firestore.collection("refresh_tokens").document(refreshToken).set(tokenRecord).get();
```
✅ Correctly stores the refresh token in Firestore and blocks until write is done.

✅ Step 7: Return tokens
```java
return ResponseEntity.ok(Map.of(
"accessToken", accessToken,
"refreshToken", refreshToken
));
```
✅ Well-formed response.


The Google Cloud Compute Engine, even with the e2-micro instance type, is not completely free and comes with a "monthly estimate." 
While it does have a free tier, it's easy to exceed the free limits, which can lead to unexpected charges. 

Another free solution that doesn't involve managing a virtual machine directly, leveraging a service that's designed 
for exactly this kind of scenario: ```render.com```.
Render is a modern platform-as-a-service (PaaS) that offers a generous free tier for web services, background workers, and databases. It's a fantastic alternative to traditional cloud VMs for a small project because it handles the server management, scaling, and most importantly for your case, provides free SSL certificates automatically.

### Deploying with Render.com

This solution is a highly recommended, modern approach for deploying a small-scale backend. It eliminates the need for manual server setup, Nginx configuration, and Certbot.
Prerequisites:
* Your Spring Boot application is ready to be deployed.
* Your code is in a Git repository (e.g., GitHub, GitLab).
* A custom domain name (e.g., yourdomain.com).
  Detailed Steps:
1. Prepare Your Spring Boot Application for Deployment:
    * Ensure your application-prod.properties is configured correctly, but you won't need to specify server.port or server.forward-headers-strategy=native as Render handles these automatically. Render will set the port through an environment variable (PORT).
    * A simple server.port=8080 can be a good default, but the environment variable will override it.
    * Make sure your pom.xml is configured to build a single executable JAR file (which it should be by default with spring-boot-maven-plugin).
2. Sign Up for Render:
    * Go to https://render.com/ and sign up with your GitHub, GitLab, or an email account.
    * Connect your Git repository provider.
3. Create a New Web Service:
    * In your Render dashboard, click New > Web Service.
    * Select your repository and the branch you want to deploy (e.g., main).
    * Render will automatically detect your project type and suggest settings.
4. Configure Your Service on Render:
    * Name: Give your service a name (e.g., supershopcart-backend).
    * Root Directory: If your project is not in the root of the repository, specify the path.
    * Runtime: Render will likely autodetect this as "Java."
    * Build Command: The default Maven build command is usually mvn clean install -DskipTests. You can customize this if needed.
    * Start Command: This is the command that runs your application after the build. It will typically be java -jar target/your-app-name.jar. Make sure the JAR name matches your project. For example: java -jar target/supershopcart-0.0.1-SNAPSHOT.jar.
    * Instance Type: Select the "Free" plan. This provides a small amount of memory and a shared CPU. Be aware that free instances may spin down after a period of inactivity and take a moment to spin back up on the next request.
    * Environment Variables: You can set your application-prod.properties values here as environment variables (e.g., jwt.secret, spring.datasource.url, etc.). This is the recommended secure practice.
5. Deploy and Access Your Service:
    * Click Create Web Service.
    * Render will clone your repository, run the build command, and deploy your application. You can watch the build logs in real-time.
    * Once the deployment is complete, Render provides a public URL for your service (e.g., https://supershopcart-backend.onrender.com). This URL is already HTTPS and secured with a free SSL certificate!
6. Add Your Custom Domain (Optional, but Recommended):
    * In the Render dashboard for your service, go to the Settings tab.
    * Scroll down to "Custom Domains" and click Add a Custom Domain.
    * Enter your domain (e.g., yourdomain.com or api.yourdomain.com).
    * Render will provide you with a DNS CNAME record.
    * Go to your domain registrar's DNS settings and add this CNAME record.
    * Render will automatically provision a free SSL certificate for your custom domain. This can take a few minutes.
      Why this is a better free solution:
* No Server Management: You don't have to worry about SSHing into a server, installing packages, or managing Nginx and Certbot. 
* Render handles the infrastructure for you.
* Automatic SSL: Render automatically provides and renews SSL certificates for both its own subdomain and any custom domains you add. This eliminates the manual setup and maintenance from Solution 2.
* Simplified Deployment: The deployment process is integrated with Git. A new commit to your main branch can trigger an automatic redeployment.
* Truly Free: The free tier on Render has clear, published limitations, so you are less likely to incur unexpected costs compared to a general-purpose cloud VM.
  This approach is highly aligned with modern CI/CD practices and provides a much smoother, and genuinely free, path to a production-ready, HTTPS-secured backend.

For a production deployment where you expect low to moderate traffic, Cloud Run is an excellent, nearly-free solution. 
The free tier is very generous. The main thing you need to be aware of is that you must have a billing account on file, 
and you should monitor your usage, especially if your application becomes more popular than you anticipate.

For this project's architecture with Firestore, Cloud Run is an ideal choice, as both services are serverless 
and designed to scale together seamlessly.

For a production deployment where you expect low to moderate traffic, Cloud Run is an excellent, nearly-free solution. 
The free tier is very generous. The main thing you need to be aware of is that you must have a billing account on file, 
and you should monitor your usage, especially if your application becomes more popular than you anticipate.
For this project's architecture with Firestore, Cloud Run is an ideal choice, 
as both services are serverless and designed to scale together seamlessly.

## Add a CORS Bean to the Security Filter Chain
This is the cleanest and most integrated way to do it. 
You define a CorsConfigurationSource bean and then tell Spring Security to use it.
- Define a CorsConfigurationSource Bean: Create a new @Bean method that returns a CorsConfigurationSource. 
  This bean will provide the CORS rules. 
  Update SecurityFilterChain: Add .cors(cors -> cors.configurationSource(corsConfigurationSource())) 
  to your HttpSecurity configuration.

Here is the complete, updated SecurityConfig class:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Add this line
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/public/**", "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    // Define the CORS configuration source bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // This is a simple configuration. In production, you should restrict origins.
        configuration.setAllowedOrigins(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

Why this is better:

* Single Point of Configuration: All of your security-related settings, including CORS, are in one place (SecurityConfig).
* Correct Filter Order: Spring Security's CorsFilter will be automatically added to the chain at the correct position, ensuring that pre-flight OPTIONS requests are handled before authentication or authorization checks.
* Security Integration: This approach leverages Spring Security's built-in CORS support, which is more robust and less prone to configuration issues than a separate WebMvcConfigurer.

##A Note on the WebMvcConfigurer Approach

The WebMvcConfigurer bean proposed before will also work, but it configures CORS at the Spring MVC level, which is a different layer. In some cases, this can lead to issues if Spring Security's filters block a request before the MVC CORS handler gets a chance to process it. For a pure REST API, integrating CORS directly into the security configuration is the more reliable and recommended practice.

- The WebMvcConfigurer bean  proposed before would also work because origins and methods are all allowed, so the configuration is very permissive.
- However, for a cleaner and more professional setup, the integrated Spring Security approach is superior.

Instead of only looking for a file on the classpath, the code now first checks for the ```FIREBASE_SERVICE_ACCOUNT_B64``` environment variable.

If it finds a value, it decodes the Base64 string into an InputStream and uses that to create the GoogleCredentials.

This new approach is much more secure and directly supports the ```docker-compose.yml``` file and the Google Cloud Secret Manager strategy you've adopted.
The code will now work seamlessly in a Docker container, where you can inject the secret directly without having to bundle the file.
