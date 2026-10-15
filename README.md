# calorie-tracker (CMPT 276 — Group 19)


## Project Abstract
Calorie Tracker is a web application that helps users track their daily calorie intake. Users can search for food, log meals, and keep records of previous days. 
The application will also include a dashboard and daily/weekly charts.

## 1. Do we have a clear understanding of the problem?
Yes, accurately calculating calorie intake for a day or meal can be challenging without a good platform or calculator. 
It can be time-consuming to find the correct calorie information for different foods.

## 2. How is this problem solved currently (if at all)?
Currently, people use existing calorie trackers, nutritional data online, food labels, or estimate their calorie intake manually. 
Applications like MyFitnessPal and Cronometer already provide similar features, but our project will focus on keeping calorie tracking simple and convenient.

## 3. How will this project make life better? Is it educational or just for entertainment?
This project can help people track the amount of calories in the food they eat and keep records of previous days. 
It can automatically retrieve calorie information from an API, calculate daily totals, save time, and make users more aware of their daily calorie intake. 
The project is mainly educational and practical.
 
## 4. Who is the target audience?
The target audience is essentially anyone, especially those who want to track their weight, follow diets, exercise regularly,
or have a convenient way to track calories.

## 5. What is the scope of your project?
The project aims to create a web application that lets users track their calorie intake through an API and keep track of current and previous records. 
Users can also manually enter calorie information for foods that are not available in the API.

## 6. Does this project have many individual features or one main feature with many subproblems? These are the 'epics' of your project. What are the epics?
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

**API:** Calorie API

**Documentation:** https://calorieapi.com/docs

