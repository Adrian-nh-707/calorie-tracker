# calorie-tracker (CMPT 276 — Group 19)

## Project Abstract
Calorie Tracker is a web application that helps users track their daily calorie intake and makes it easier and more convenient. Users can search for food, log meals, and keep records of previous days. 
The application will include a dashboard and daily/weekly charts, and automatically calculate calorie totals based on servings and allows users to enter custom calorie values when the nutritional info is not available. There will be different roles to help users track and manage their calories, such as a trainer role.

## 1. Do we have a clear understanding of the problem?
There is a challenge in accurately calculating the calorie intake in a day/meal without a good platform/calculators already implemented. It can be time consuming to search for nutritional info, serving sizes, and calculate their total calorie consumption. The project tries to address the challenges by combining food searches, meal logging and historical tracking.

## 2. How is this problem solved currently (if at all)?
Currently, people use existing calorie trackers, nutritional data online, food labels, or estimate their calorie intake manually. Applications like MyFitnessPal and Cronometer already provide similar features, but our project will focus on keeping calorie tracking simple and all in one app, keeping it straightforward. We also plan on adding food recommendations based on remaining calories and a trainer role that allows trainers to give recommendations to specific users.

## 3. How will this project make life better? Is it educational or just for entertainment?
This project can help people accurately track the amount of calories that are in the food that they are eating by making it easier to record and review their daily calorie intake. This project can help people track the amount of calories in the food they eat and keep records of previous days, it can automatically retrieve calorie information from an API, calculate daily totals, save time, and make users more aware of their daily calorie intake. The app will also recommend food based on remaining calories to help users find foods that fit within their calorie goals, with trainers being able to provide more recommendations to specific users.
The project is mainly educational and practical, helping users understand and monitor their food intake rather than giving medical advice.
 
## 4. Who is the target audience?
Target audience is university students, fitness enthusiasts, those who want a convenient way to track calories. We specifically want to help people who find that manual calorie tracking is time consuming and want a simple way to record meals and review their progress.

## 5. What is the scope of your project?
The project aims to make a web app that lets users track and record their calorie intake through an API, view historical calorie intake. The app will take calorie info from an API and calculate totals based on servings. Users can also manually enter calorie information for foods that are not available in the API. The app will also provide personal calorie goals daily, with a dashboard showing the current calories and remaining intake, historical records with daily and weekly charts. It will recommend food that are based on the remaining daily calories left. There will be three user roles: regular users, trainers, and administrators.

## 6. Does this project have many individual features or one main feature with many subproblems? These are the 'epics' of your project. What are the epics?
The project will be divided into five major features (epics), with each member responsible for the development of the feature: 
1. User Management: Registration, authentication, roles, user profiles, nutrition goals
2. Food Search and Meal Logging: Food API, searching foods, logging meals, portions, calorie calculations
3. Dashboard and Analytics: Daily calorie totals, macros, progress tracking, nutrition history
4. Smart Food Recommendations: Food suggestions based on remaining calories, protein, dietary preferences
5. Meal Planning: Custom recipes, saved meals, weekly meal plans, shopping lists
The project will be divided into five major features:

### 1. User Accounts & Personalization
Users can create accounts, log in, and set personal calorie goals.

### 2. API Integration
Retrieve nutritional information about food items through a REST API.

### 3. Meal Logging (Calculations)
Allow users to record meals, enter food quantities, and calculate daily calorie totals.

### 4. Dashboard
Display daily calorie intake and progress toward calorie goals.

### 5. Analytics (History & Daily/Weekly Charts)
Allow users to view previous records and daily/weekly calorie charts.

## 7. Is the amount of work sufficient for five group members? A rough rule of thumb is that each group member should have one major feature.
Yes, the amount of work is enough for five group members. The project is divded into five major features, which means each member takes resposibility for one feature. 
All the features have distinctive dev tasks, with group collaboration needed to connect the database/api and integrate the features as a whole, and test the app.

## 8. REST API
Our group is considering using the Calorie API to retrieve nutritional information about different food items. 
The API supports food search, barcode lookup, and food details. It requires an API key sent through the X-API-Key header and provides access to a catalog of over 4 million foods.

## 9. Roles within the App
Users will be able to create an account, manage their profile, and set personal calorie and macronutrient goals. They can search for foods, view nutritional information, log meals, and track their daily intake through a personalized dashboard. Users can also review their nutrition history, receive food recommendations based on their goals, and create meal plans with saved recipes and shopping lists to help them manage their eating habits.

Trainers will be able to connect with regular users through the application and monitor their nutritional progress, including daily calorie intake, macronutrients, and progress toward personal goals. They can provide feedback, suggest adjustments to calorie and nutrition targets, and help the regular user stay accountable through regular check-ins. the user can accept or decline proposed goal changes, allowing them to work collaboratively with their trainers toward their individual fitness and nutrition goals.

Admin manages user accounts (trainers or regular users). able to insert / delete data. Connects users to trainers or vice versa. modify user/trainer dashboards. Deals with maintenance.  Admin dashboard will contain user/trainer list of info, which is basically an overseer for its users.

**API:** Calorie API

**Documentation:** https://calorieapi.com/docs
