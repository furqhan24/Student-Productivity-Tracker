# Student Productivity Tracker

A desktop-based productivity management application developed using Java Swing, JDBC, and Oracle Database. The system helps users plan activities, track completed work, analyze productivity, and generate recommendations for improving time management.

## Overview

The application is designed around a simple idea: every day consists of a limited number of hours.

Users can:

* Create daily schedules
* Log completed activities
* Compare planned work against actual work
* Analyze productive and unproductive time
* Generate improvement recommendations

The system promotes productivity awareness by helping users understand how their time is spent and identify opportunities for improvement.

## Features

### Authentication Module

* User Registration
* User Login
* Credential Validation

### Schedule Management

* Add Schedule Entries
* Update Schedule Entries
* Delete Schedule Entries
* View Daily Schedules
* Daily planning hour validation

### Daily Activity Logging

* Record completed activities
* Modify activity logs
* Delete log entries
* Track actual time spent

### Productivity Analytics

* Productive vs Unproductive Hour Analysis
* Activity-wise Productivity Reports
* SubActivity-wise Analysis
* Data-driven insights using SQL queries

### Recovery Planning

* Productivity improvement recommendations
* Analysis-based suggestions
* Performance comparison between planned and actual work

### Calendar Tracking

* Date-wise activity history
* Productivity review by day
* Activity timeline tracking

## Technology Stack

### Frontend

* Java Swing

### Backend

* Oracle Database

### Database Connectivity

* JDBC

### Architecture

* Desktop Application
* Database-Driven Design

## Database Design

The system follows normalization principles and maintains data consistency through relational database design.

### Core Entities

#### Student

Stores user account information.

#### Activity

Stores primary activity categories such as:

* Study
* Exercise
* Sleep
* Gaming

#### SubActivity

Stores detailed classifications under activities.

Examples:

Study

* DBMS
* Java
* Mathematics

Exercise

* Cardio
* Running
* Weight Training

#### Schedule

Stores planned activities and allocated hours.

#### Daily Log

Stores actual activities performed and hours spent.

## SQL Concepts Implemented

* CREATE TABLE
* INSERT
* UPDATE
* DELETE
* SELECT
* JOIN
* LEFT JOIN
* GROUP BY
* ORDER BY
* CASE
* SUM
* MIN
* MAX
* Views
* Primary Keys
* Foreign Keys

## Key Features Implemented

* JDBC Database Connectivity
* CRUD Operations
* Authentication System
* Relational Database Design
* Productivity Analytics
* Data Validation
* Business Rule Enforcement

## Business Rules

* Total planned hours for a day cannot exceed 24.
* Total logged hours for a day cannot exceed 24.
* Duplicate activity combinations are merged.
* Referential integrity is maintained through foreign keys.

## Learning Outcomes

This project helped strengthen understanding of:

* Java Swing Development
* JDBC Connectivity
* Oracle SQL
* Database Normalization
* Relational Database Design
* SQL Query Optimization
* CRUD Application Development
* Software Design Principles

## Future Enhancements

* Productivity Score Calculation
* Weekly and Monthly Trend Analysis
* Interactive Charts and Dashboards
* Goal-Based Productivity Tracking
* AI-Powered Recommendations
* Cloud Database Integration

## Author

Mohammed Furqhan

GitHub: https://github.com/furqhan24
