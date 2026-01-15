# Food Donation App

A food donation application built with Android Jetpack Compose and Supabase, connecting food donors with receivers.

## Features

- Email/Password authentication
- Two user roles: Donor and Receiver
- Modern UI with Jetpack Compose
- Supabase backend integration

## Setup Instructions

### 1. Supabase Configuration

1. Create a Supabase project at [supabase.com](https://supabase.com)
2. Get your Supabase URL and anon key from Project Settings > API
3. Update the credentials in `app/src/main/java/com/it/fooddonation/data/remote/SupabaseClient.kt`:
   ```kotlin
   private const val SUPABASE_URL = "your-project-url.supabase.co"
   private const val SUPABASE_KEY = "your-anon-key"
   ```

### 2. Database Setup

Create a `profiles` table in your Supabase database with the following schema:

```sql
create table profiles (
  id uuid references auth.users on delete cascade primary key,
  email text not null,
  full_name text,
  phone_number text,
  role text not null check (role in ('donor', 'receiver')),
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Enable Row Level Security
alter table profiles enable row level security;

-- Allow users to read their own profile
create policy "Users can view own profile"
  on profiles for select
  using (auth.uid() = id);

-- Allow users to insert their own profile
create policy "Users can insert own profile"
  on profiles for insert
  with check (auth.uid() = id);

-- Allow users to update their own profile
create policy "Users can update own profile"
  on profiles for update
  using (auth.uid() = id);
```

### 3. Enable Email Authentication

In your Supabase project:
1. Go to Authentication > Providers
2. Enable Email provider
3. Configure email templates as needed

## Project Structure

```
app/src/main/java/com/it/fooddonation/
├── data/
│   ├── model/
│   │   ├── UserRole.kt
│   │   └── UserProfile.kt
│   ├── remote/
│   │   └── SupabaseClient.kt
│   └── repository/
│       └── AuthRepository.kt
├── navigation/
│   ├── NavGraph.kt
│   └── Screen.kt
├── ui/
│   ├── auth/
│   │   ├── viewmodel/
│   │   │   └── AuthViewModel.kt
│   │   ├── LoginScreen.kt
│   │   └── RegisterScreen.kt
│   ├── home/
│   │   └── HomeScreen.kt
│   └── theme/
└── MainActivity.kt
```

## Dependencies

- Supabase Kotlin SDK
- Jetpack Compose
- Navigation Compose
- ViewModel

## Build and Run

1. Sync Gradle dependencies
2. Update Supabase credentials
3. Run the app on an emulator or device

```bash
./gradlew assembleDebug
```

## User Roles

- **Donor**: Users who want to donate food
- **Receiver**: Organizations or individuals receiving food donations
