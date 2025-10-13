# Phase II: Password Reset & Firebase Integration Plan

**Date:** 2025-10-12
**Project:** AITripPlanner
**Branch:** fanshi/recommendation

## Executive Summary

This plan outlines the implementation of password reset functionality and optional Firebase authentication integration for the AITripPlanner frontend. The backend already has password reset endpoints implemented; this phase focuses on building the UI/UX for the reset flow and optionally integrating Firebase authentication alongside the existing JWT-based system.

---

## Current State Analysis

### Backend (Spring Boot) ✅
- **Authentication Controller:** `/api/auth/*`
- **Implemented Endpoints:**
  - `POST /api/auth/register` - User registration
  - `POST /api/auth/login` - Login with email/password (returns JWT)
  - `POST /api/auth/forgot-password` - Initiates password reset (sends email)
  - `POST /api/auth/reset-password` - Validates token & resets password
- **Email Service:** Configured and working
- **Location:** `src/main/java/org/laioffer/planner/user/authentication/`

### Frontend - Current (AITripPlannerFE/trip-mate)

**Technology Stack:**
- React 19.1.1
- Material-UI (MUI) 7.3.2
- React Router DOM 7.9.1
- Axios 1.12.0
- Firebase 12.2.1 (installed but unused)

**Authentication Implementation:**
- **Method:** Backend JWT tokens stored in localStorage
- **Components:**
  - `LoginModal.js` - Email/password login with tabs for Sign In/Sign Up
  - `AuthContext.js` - Context provider for auth state
  - `lib/auth.js` - Auth functions (login, register, logout, token management)
  - Password reset API functions exist (lines 119-146) but no UI
- **Missing:**
  - "Forgot Password?" link in login UI
  - Password reset page/dialog components
  - Token auto-refresh on 401 errors

### Frontend - Firebase Version (AITripPlannerFE_firebase) - For Reference

**Authentication Implementation:**
- **Method:** Google OAuth only via Firebase Authentication
- **Key Features:**
  - Simple `signInWithGoogle()` popup flow
  - `onAuthStateChanged` for session management
  - **Token auto-refresh interceptor** on 401 errors (very useful!)
  - API client automatically refreshes Firebase tokens
- **Location:** `AITripPlannerFE_firebase/src/`
  - `lib/auth.js` - Firebase auth wrapper
  - `api/index.js:11-32` - Token refresh interceptor ⭐ Key reference

**What to Borrow:**
1. Token auto-refresh pattern from `api/index.js`
2. Clean auth state management approach
3. Simple OAuth implementation pattern (for future Phase 3)

---

## Implementation Plan

### Phase 1: Password Reset UI (Priority - Current Sprint)

#### 1.1 Update LoginModal Component
**File:** `AITripPlannerFE/trip-mate/src/components/auth/LoginModal.js`

**Changes:**
- Add "Forgot Password?" link below password field in Sign In tab
- Add state for forgot password dialog:
  ```javascript
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  ```
- Position link between password field and Sign In button
- Style as subtle text link (MUI Link component)

**Visual Placement:**
```
[Email Field]
[Password Field]
Forgot Password? ← Add this link
[Sign In Button]
```

#### 1.2 Create ForgotPasswordDialog Component
**New File:** `AITripPlannerFE/trip-mate/src/components/auth/ForgotPasswordDialog.js`

**Purpose:** Modal dialog for initiating password reset

**Features:**
- Email input field (with validation)
- Submit button calls `forgotPassword(email)` from lib/auth.js
- Loading state during API call
- Success message: "Password reset link sent to your email"
- Error handling with Alert component
- Close button

**Component Structure:**
```javascript
export default function ForgotPasswordDialog({ open, onClose }) {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    const result = await forgotPassword(email);
    // Handle result...
  };

  return (
    <Dialog open={open} onClose={onClose}>
      {/* Form implementation */}
    </Dialog>
  );
}
```

#### 1.3 Create ResetPasswordPage Component
**New File:** `AITripPlannerFE/trip-mate/src/pages/ResetPassword/index.js`

**Purpose:** Public page for password reset (accessed via email link)

**Features:**
- Extract token from URL query parameter: `?token=xxx`
- Form with two fields:
  - New Password (type="password")
  - Confirm Password (type="password")
- Password validation:
  - Minimum length (8 characters)
  - Match confirmation
  - Strength indicator (optional)
- Submit calls `resetPassword(token, newPassword)` from lib/auth.js
- Success: Show success message + link to login
- Error: Show error message (invalid/expired token)
- Redirect to home after 3 seconds on success

**URL Structure:**
```
https://yourapp.com/reset-password?token=abc123xyz
```

#### 1.4 Add Route Configuration
**File:** `AITripPlannerFE/trip-mate/src/routes/index.js`

**Changes:**
```javascript
import ResetPassword from "../pages/ResetPassword";

// Add public route (outside RequireAuth wrapper)
<Route path="reset-password" element={<ResetPassword />} />
```

**Route Priority:** Place before the `path="*"` catch-all route

#### 1.5 Update AuthContext (Optional)
**File:** `AITripPlannerFE/trip-mate/src/auth/AuthContext.js`

**Changes (if needed):**
- Expose forgotPassword and resetPassword functions in context
- Currently these are imported directly from lib/auth.js, which is fine
- Only update if you want centralized auth method access

---

### Phase 2: Token Auto-Refresh (Recommended - Next Sprint)

#### 2.1 Create/Update API Client
**File:** `AITripPlannerFE/trip-mate/src/api/index.js` (create if doesn't exist)

**Purpose:** Centralized axios instance with interceptors

**Reference Implementation:** `AITripPlannerFE_firebase/src/api/index.js:11-32`

**Implementation:**
```javascript
import axios from 'axios';
import { getAuthToken, setAuthToken, removeAuthToken, refreshAuthToken } from '../lib/auth';

const API_BASE = process.env.REACT_APP_API_BASE || "/api";

const api = axios.create({
  baseURL: API_BASE,
});

// Request interceptor - attach token to all requests
api.interceptors.request.use(
  (config) => {
    const token = getAuthToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401 errors
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Attempt to refresh token
        const newToken = await refreshAuthToken();
        if (newToken) {
          setAuthToken(newToken);
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return api.request(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed - logout user
        removeAuthToken();
        window.location.href = '/';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

#### 2.2 Add Token Refresh Function
**File:** `AITripPlannerFE/trip-mate/src/lib/auth.js`

**New Function:**
```javascript
export async function refreshAuthToken() {
  try {
    const currentToken = getAuthToken();
    if (!currentToken) return null;

    const response = await axios.post(`${API_BASE}/auth/refresh`, {
      token: currentToken
    });

    return response.data.token;
  } catch (error) {
    console.error('Token refresh failed:', error);
    return null;
  }
}
```

**Backend Requirement:**
- Need `POST /api/auth/refresh` endpoint (if not already implemented)
- Validates current token, returns new token if valid

#### 2.3 Update All API Calls
**Files:** All components making API calls

**Changes:**
- Import centralized `api` instance instead of axios
- Remove manual token attachment (handled by interceptor)
- API calls remain the same, just use `api` instead of `axios`

---

### Phase 3: Google Sign-In Integration (Optional - Future Sprint)

#### 3.1 Update LoginModal for Google OAuth
**File:** `AITripPlannerFE/trip-mate/src/components/auth/LoginModal.js`

**Changes:**
- Add "Sign in with Google" button above email/password form
- Import `signInWithGoogle` from Firebase auth
- Handle Google sign-in flow:
  1. User clicks "Sign in with Google"
  2. Firebase popup authentication
  3. Get Firebase ID token
  4. Send to backend `/api/auth/google-login`
  5. Backend returns JWT token
  6. Store JWT and update auth state

**Implementation:**
```javascript
import { signInWithGoogle as firebaseGoogleSignIn } from '../../lib/firebase-auth';

const handleGoogleSignIn = async () => {
  setLoading(true);
  try {
    const firebaseUser = await firebaseGoogleSignIn();
    const idToken = await firebaseUser.getIdToken();

    // Send to backend for JWT
    const response = await axios.post('/api/auth/google-login', {
      firebaseToken: idToken
    });

    const jwtToken = response.data.token;
    setAuthToken(jwtToken);
    onSuccess?.();
  } catch (error) {
    setError(error.message);
  } finally {
    setLoading(false);
  }
};
```

#### 3.2 Create Firebase Auth Helper
**New File:** `AITripPlannerFE/trip-mate/src/lib/firebase-auth.js`

**Purpose:** Wrapper for Firebase OAuth functions

**Content:**
```javascript
import { GoogleAuthProvider, signInWithPopup } from "firebase/auth";
import { auth } from "./firebase";

export async function signInWithGoogle() {
  const provider = new GoogleAuthProvider();
  provider.addScope("email");
  provider.addScope("profile");
  const result = await signInWithPopup(auth, provider);
  return result.user;
}
```

#### 3.3 Backend Implementation
**New Endpoint:** `POST /api/auth/google-login`

**Java Implementation:**
```java
@PostMapping("/google-login")
public LoginResponse googleLogin(@Valid @RequestBody GoogleLoginRequest body) {
    // 1. Verify Firebase ID token using Firebase Admin SDK
    // 2. Extract user email from token
    // 3. Find or create user in database
    // 4. Generate backend JWT token
    // 5. Return JWT token
    return new LoginResponse(jwtToken);
}
```

**Requirements:**
- Add Firebase Admin SDK dependency to backend
- Verify Firebase ID tokens server-side
- Sync Firebase users with local database

---

## File Structure

```
AITripPlannerFE/trip-mate/src/
├── components/
│   └── auth/
│       ├── LoginModal.js (UPDATE)
│       ├── LoginForm.js
│       └── ForgotPasswordDialog.js (NEW)
├── pages/
│   ├── Home/
│   ├── Setup/
│   └── ResetPassword/ (NEW)
│       └── index.js
├── routes/
│   └── index.js (UPDATE)
├── auth/
│   └── AuthContext.js (optional UPDATE)
├── lib/
│   ├── auth.js (UPDATE - add refreshAuthToken)
│   ├── firebase.js (exists)
│   └── firebase-auth.js (NEW for Phase 3)
└── api/
    └── index.js (NEW - centralized axios instance)
```

---

## Dependencies

### Current Dependencies ✅
All required packages already installed:
- `firebase@12.2.1`
- `axios@1.12.0`
- `@mui/material@7.3.2`
- `react-router-dom@7.9.1`

### No New Dependencies Required

---

## Backend API Contracts

### Existing Endpoints (Already Implemented)

#### 1. Forgot Password
```
POST /api/auth/forgot-password
Content-Type: application/json

Request:
{
  "email": "user@example.com"
}

Response: 200 OK
{
  "message": "Password reset link has been sent to your email"
}
```

#### 2. Reset Password
```
POST /api/auth/reset-password
Content-Type: application/json

Request:
{
  "token": "reset-token-from-email",
  "newPassword": "newSecurePassword123"
}

Response: 200 OK
{
  "message": "Password has been reset successfully"
}
```

### New Endpoints Needed

#### 3. Token Refresh (Phase 2)
```
POST /api/auth/refresh
Content-Type: application/json
Authorization: Bearer <current-jwt-token>

Response: 200 OK
{
  "token": "new-jwt-token"
}

Error: 401 Unauthorized
```

#### 4. Google Login (Phase 3)
```
POST /api/auth/google-login
Content-Type: application/json

Request:
{
  "firebaseToken": "firebase-id-token"
}

Response: 200 OK
{
  "token": "backend-jwt-token"
}

Error: 401 Unauthorized
```

---

## Testing Plan

### Phase 1 Testing
1. **Forgot Password Flow:**
   - Click "Forgot Password?" link
   - Enter valid email → verify email sent
   - Enter invalid email → verify error handling
   - Check email inbox for reset link

2. **Reset Password Flow:**
   - Click reset link from email
   - Enter new password → verify success
   - Enter mismatched passwords → verify validation
   - Use expired token → verify error message
   - Use invalid token → verify error message

3. **Integration:**
   - Reset password → login with new password
   - Verify old password no longer works

### Phase 2 Testing
1. **Token Refresh:**
   - Make API call with valid token → succeeds
   - Make API call with expired token → auto-refreshes → succeeds
   - Make API call with invalid token → redirects to login

2. **Edge Cases:**
   - Concurrent requests with expired token
   - Network failure during refresh
   - Refresh endpoint returns 401

### Phase 3 Testing
1. **Google Sign-In:**
   - Click "Sign in with Google" → popup appears
   - Select Google account → successful login
   - Cancel popup → no error
   - Verify user synced to database

---

## Security Considerations

### Password Reset Security
- ✅ Reset tokens should expire (backend responsibility)
- ✅ One-time use tokens (backend responsibility)
- ✅ Send reset link only to registered emails
- ⚠️ Frontend: Clear token from URL after use
- ⚠️ Frontend: No sensitive data in error messages

### Token Management
- ✅ Store JWT in localStorage (already implemented)
- ✅ Remove token on logout
- ✅ Validate token before making requests
- ⚠️ Consider httpOnly cookies for future (more secure)

### Firebase Integration
- ✅ Verify Firebase tokens on backend (never trust client)
- ✅ Use Firebase Admin SDK for verification
- ✅ Don't expose Firebase config secrets

---

## Migration Strategy

### Phase 1: Low Risk ✅
- Pure UI additions, no breaking changes
- Backend endpoints already exist
- Can be deployed independently

### Phase 2: Medium Risk ⚠️
- Changes to API client affect all requests
- Test thoroughly in development
- Have rollback plan (remove interceptor)
- Deploy during low-traffic period

### Phase 3: High Risk 🔴
- Requires backend changes
- New authentication flow
- Test extensively before production
- Consider feature flag for gradual rollout

---

## Timeline Estimates

### Phase 1: Password Reset UI
- **Development:** 2-3 days
- **Testing:** 1 day
- **Total:** 3-4 days

### Phase 2: Token Auto-Refresh
- **Development:** 1-2 days
- **Testing:** 1 day
- **Total:** 2-3 days

### Phase 3: Google Sign-In
- **Frontend Development:** 2 days
- **Backend Development:** 2-3 days
- **Testing:** 2 days
- **Total:** 6-7 days

**Overall Estimate:** 11-14 days (if all phases implemented)

---

## Success Metrics

### Phase 1
- ✅ Users can successfully reset forgotten passwords
- ✅ Email delivery success rate > 95%
- ✅ Password reset completion rate > 60%
- ✅ Zero errors in password reset flow

### Phase 2
- ✅ Reduction in unexpected logouts
- ✅ Token refresh success rate > 99%
- ✅ Improved user session duration

### Phase 3
- ✅ Google sign-in success rate > 90%
- ✅ 30%+ users opt for Google sign-in
- ✅ Reduced registration abandonment

---

## References

### Internal Documentation
- Phase I Documentation: `doc/PhaseI/`
- Backend API: `src/main/java/org/laioffer/planner/user/authentication/`
- Firebase Version: `AITripPlannerFE_firebase/`

### Key Files Referenced
- `AITripPlannerFE_firebase/src/api/index.js` - Token refresh pattern
- `AITripPlannerFE_firebase/src/lib/auth.js` - Firebase auth wrapper
- `AITripPlannerFE/trip-mate/src/lib/auth.js` - Current auth implementation

### External Resources
- [Firebase Authentication Docs](https://firebase.google.com/docs/auth)
- [Axios Interceptors](https://axios-http.com/docs/interceptors)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

---

## Appendix: Component Mockups

### LoginModal with Forgot Password Link
```
┌─────────────────────────────────────┐
│            Welcome                  │
│                                     │
│  [Sign In]  [Sign Up]              │
│                                     │
│  Welcome Back                       │
│                                     │
│  Email*                            │
│  [________________]                 │
│                                     │
│  Password*                          │
│  [________________]                 │
│                                     │
│  Forgot Password? ← NEW             │
│                                     │
│  [    Sign In    ]                 │
└─────────────────────────────────────┘
```

### Forgot Password Dialog
```
┌─────────────────────────────────────┐
│  Reset Password              [X]    │
│                                     │
│  Enter your email address and      │
│  we'll send you a link to reset    │
│  your password.                     │
│                                     │
│  Email*                            │
│  [________________]                 │
│                                     │
│  [   Send Reset Link   ]           │
└─────────────────────────────────────┘
```

### Reset Password Page
```
┌─────────────────────────────────────┐
│  Travel Planner                     │
│                                     │
│  Reset Your Password                │
│                                     │
│  New Password*                      │
│  [________________]                 │
│                                     │
│  Confirm Password*                  │
│  [________________]                 │
│                                     │
│  [  Reset Password  ]              │
│                                     │
│  Back to Login                      │
└─────────────────────────────────────┘
```

---

**Document Version:** 1.0
**Last Updated:** 2025-10-12
**Status:** Ready for Implementation
