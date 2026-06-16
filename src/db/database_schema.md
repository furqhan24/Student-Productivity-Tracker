# Productivity Tracking System - Database Schema

## Entity Relationships

CATEGORY (1) ----< ACTIVITY (M)

ACTIVITY (1) ----< SUB_ACTIVITY (M)

STUDENT (1) ----< SCHEDULE (M)

STUDENT (1) ----< DAILY_LOG (M)

ACTIVITY (1) ----< SCHEDULE (M)

ACTIVITY (1) ----< DAILY_LOG (M)

SUB_ACTIVITY (1) ----< SCHEDULE (M)

SUB_ACTIVITY (1) ----< DAILY_LOG (M)

---

## Table Descriptions

### STUDENT

Stores user account information and login credentials.

### CATEGORY

Stores high-level classifications such as Productive and Unproductive.

### ACTIVITY

Stores major activities such as Study, Exercise, Gaming, and Social Media.

### SUB_ACTIVITY

Stores detailed activities such as DBMS, Java, Cardio, Weight Training, and user-created custom activities.

### SCHEDULE

Stores planned activities, subactivities, dates, and allocated hours.

### DAILY_LOG

Stores actual activities performed along with time spent.

---

## Project Workflow

Student
→ Creates Schedule
→ Performs Activities
→ Records Daily Logs
→ Generates Productivity Insights

The system helps users compare planned versus actual work, track time usage, analyze productivity patterns, and improve time management.
