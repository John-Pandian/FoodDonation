-- Create profiles table
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
