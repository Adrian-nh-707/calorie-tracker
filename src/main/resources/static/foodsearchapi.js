/*
Food database, API integration, and food search

*/

    function parseRecipe(recipe_arr){
        let recipe = "";
        for(let i = 0; i < recipe_arr.length; i++){
            recipe += recipe_arr[i] + " ";
        }
        return recipe;
        
    }
    function parseIngredients(ingredient_arr){
        let ingredients = "";
        for(let i = 0; i < ingredient_arr.length; i++){
            ingredients += "" + ingredient_arr[i]["name"] + "(" + ingredient_arr[i]["quantity"] + " " + ingredient_arr[i]["unit"] + "). ";
        }



        return ingredients;
    }

    function showNutrition(num){
        let nutrition_text = document.getElementById("nutrition" + num);
        let ingredient_text = document.getElementById("ingredients" + num);
        let recipe_text = document.getElementById("recipe" + num);
        //console.log(nutrition_text.style.display);
        if(nutrition_text.style.display == "none"){
            nutrition_text.style.display = "";
            ingredient_text.style.display = "none";
            recipe_text.style.display = "none"
        }

    }
    function showIngredients(num){
        let nutrition_text = document.getElementById("nutrition" + num);
        let ingredient_text = document.getElementById("ingredients" + num);
        let recipe_text = document.getElementById("recipe" + num);

        if(ingredient_text.style.display == "none"){
            ingredient_text.style.display = "";
            nutrition_text.style.display = "none";
            recipe_text.style.display = "none"
        }
    }
    function showRecipe(num){
        let nutrition_text = document.getElementById("nutrition" + num);
        let ingredient_text = document.getElementById("ingredients" + num);
        let recipe_text = document.getElementById("recipe" + num);

        if(recipe_text.style.display == "none"){
            recipe_text.style.display = "";
            ingredient_text.style.display = "none";
            nutrition_text.style.display = "none"
        }
    }


    async function submitSearch(event){
        event.preventDefault();
        document.getElementById("item_list").innerHTML = "";
        let search = document.getElementById("search").value;
        document.getElementById("search").value = "";
        console.log(search);

        await getFoodList(search);
    }


    async function getFoodList(search){

    let parse_str = search.replace(/\s+/g, '%20');

    console.log(parse_str);
   

    try{
        //spinner.style.display = "block"
        const apiKey = 'OEMTy5t2BzkBRD1Y0JxMN4kqBbZFdLmNjf6X5CPH';

        const response = await fetch(`https://api.api-ninjas.com/v3/recipe?title=${parse_str}&limit=5`, {
              headers: {
                'X-Api-Key': apiKey,
                'Content-Type': 'application/json'
              }

            });

        if (!response.ok) {
            const errorText = await response.text(); // Get raw text to see server errors
            throw new Error(`API returned status ${response.status}: ${errorText}`);
        }

        const data = await response.json();
        console.log(data);

        /* single entry format:
        
            {title: "Elisa's Almost Fat-Free Potato Latkes", ingredients: Array(5), servings: '1 Servings', instructions: Array(7), nutrition: {…}}
            ingredients: (5) [{…}, {…}, {…}, {…}, {…}]

            instructions: (7) ['Shred the potatoes in the food processor.', 'Rinse the potatoes.', 'Toss the potatoes in a bowl with the pepper, dried onion flakes, and egg.', 'Mix until combined.', 'Line a pan with aluminum foil and spray with PAM.', 'Divide the potato mixture into 6 mounds on the pan.', 'Spray the mounds with PAM.'] 

            nutrition: {calories: 3677.503181, total_fat: 345.178095, saturated_fat: 36.215579, protein: 38.662521, sodium: 671.590869, …}
            
            servings: "1 Servings"

            title: "Elisa's Almost Fat-Free Potato Latkes"

            [[Prototype]]
            : 
            Object


        */

        /*

            "nutrition": {
            "calories": 2459.801609,
            "total_fat": 133.421434,
            "saturated_fat": 56.038221,
            "protein": 34.051346,
            "sodium": 1093.891319,
            "potassium": 506.742703,
            "dietary_fiber": 20.048984,
            "cholesterol": 202.196154,
            "sugars": 69.58574,
            "total_carbohydrate": 287.809611
            }


        */

        let container = document.getElementById("item_list");

        for(let i = 0; i < data.length; i++){
            console.log(data[i]);

            let item_container = document.createElement("div");
            //item_container.innerHTML = "hello";
            item_container.classList.add("col-md-6","mb-4");
            //console.log(item_container.className);

            let item_container_main = document.createElement("div"); //meal content page
            item_container_main.classList.add("p-3","bg-light","border");

            let item_container_add = document.createElement("div"); //where the add/submit button is
            item_container_main.classList.add("p-3","bg-light","border");

            let item_ingredients = document.createElement("div"); // list of ingredients;
            item_ingredients.style.display = "none";
            let ingredient_title = document.createElement("h3");
            ingredient_title.innerHTML = "Ingredients:"
            item_ingredients.appendChild(ingredient_title);
            item_ingredients.id = "ingredients" + i;

            let item_nutrition = document.createElement("div"); // list of nutrition (calories, protein, etc)
            item_nutrition.style.display = "none";
            let nutrition_title = document.createElement("h3");
            nutrition_title.innerHTML = "Nutrition:";
            item_nutrition.appendChild(nutrition_title);
            item_nutrition.id = "nutrition" + i;

            let item_recipe = document.createElement("div"); // recipe
            let recipe_title = document.createElement("h3");
            recipe_title.innerHTML = "Recipe:";
            item_recipe.appendChild(recipe_title);
            item_recipe.id = "recipe" + i;

            for(let j = 0; j < data[i]["ingredients"].length; j++){
                let current_ingredient = document.createElement("p");
                current_ingredient.innerHTML = "" + data[i]["ingredients"][j]["name"] + ": " + data[i]["ingredients"][j]["quantity"] + " " + data[i]["ingredients"][j]["unit"] + ".";
                item_ingredients.appendChild(current_ingredient);
            }

            for(const [key, value] of Object.entries(data[i]["nutrition"])){
                let current_nutrition = document.createElement("p");
                current_nutrition.innerHTML = "" + key + ":  " + value + "g.";
                item_nutrition.appendChild(current_nutrition);
            }

            for(let j = 0; j < data[i]["instructions"].length; j++){
                let current_recipe = document.createElement("p");
                current_recipe.innerHTML = "" + (j+1) + ":  " + data[i]["instructions"][j];
                item_recipe.appendChild(current_recipe);
            }




            

            let title = document.createElement("h3");
            title.innerHTML = data[i]["title"];
            title.style.border = "solid green";
            title.style.textAlign = "center";


            let recipe_str = parseRecipe(data[i]["instructions"]);
            let ingredients_str = parseIngredients(data[i]["ingredients"]);
            
            
            let form = document.createElement("form");
            form.style.float = "right";
            form.method = "post";
            form.action = "food/add";

            let form_title = document.createElement("input");
            form_title.value = data[i]["title"];
            form_title.name = "title";
            form_title.style.display = "none";

            let serving = document.createElement("input");
            serving.value = data[i]["servings"];
            serving.name = "serving";
            serving.style.display = "none";


            let calories = document.createElement("input");
            calories.value = data[i]["nutrition"]["calories"];
            calories.name = "calories";
            calories.style.display = "none";

            let total_fat = document.createElement("input");
            total_fat.value = data[i]["nutrition"]["total_fat"];
            total_fat.name = "total_fat";
            total_fat.style.display = "none";

            let saturated_fat = document.createElement("input");
            saturated_fat.value = data[i]["nutrition"]["saturated_fat"];
            saturated_fat.name = "saturated_fat";
            saturated_fat.style.display = "none";

            let protein = document.createElement("input");
            protein.value = data[i]["nutrition"]["protein"];
            protein.name = "protein";
            protein.style.display = "none";

            let sodium = document.createElement("input");
            sodium.value = data[i]["nutrition"]["sodium"];
            sodium.name = "sodium";
            sodium.style.display = "none";

            let potassium = document.createElement("input");
            potassium.value = data[i]["nutrition"]["potassium"];
            potassium.name = "potassium";
            potassium.style.display = "none";


            let dietary_fiber = document.createElement("input");
            dietary_fiber.value = data[i]["nutrition"]["dietary_fiber"];
            dietary_fiber.name = "dietary_fiber";
            dietary_fiber.style.display = "none";

            let cholesterol = document.createElement("input");
            cholesterol.value = data[i]["nutrition"]["cholesterol"];
            cholesterol.name = "cholesterol"
            cholesterol.style.display = "none";

            let sugars = document.createElement("input");
            sugars.value = data[i]["nutrition"]["sugars"];
            sugars.name = "sugars"
            sugars.style.display = "none";

            let total_carbohydrate = document.createElement("input");
            total_carbohydrate.value = data[i]["nutrition"]["total_carbohydrate"];
            total_carbohydrate.name = "total_carbohydrate";
            total_carbohydrate.style.display = "none";

            let recipe = document.createElement("input");
            recipe.value = recipe_str;
            recipe.name = "recipe"
            recipe.style.display = "none";

            let ingredients = document.createElement("input");
            ingredients.value = ingredients_str;
            ingredients.name = "ingredients"
            ingredients.style.display = "none";
            
            let submit = document.createElement("button");
            submit.type = "submit";
            submit.innerHTML = "+";
            
            form.appendChild(form_title);
            form.appendChild(serving);
            form.appendChild(calories);
            form.appendChild(total_fat);
            form.appendChild(saturated_fat);
            form.appendChild(protein);
            form.appendChild(sodium);
            form.appendChild(potassium);
            form.appendChild(dietary_fiber);
            form.appendChild(cholesterol);
            form.appendChild(sugars);
            form.appendChild(total_carbohydrate);
            form.appendChild(recipe);
            form.appendChild(ingredients);
            form.appendChild(submit);

            
            
            
            
            //item_container_main.appendChild(title);
            let nutrition_button = document.createElement("button");
            nutrition_button.innerHTML = "Nutrition";
            nutrition_button.onclick = function(){
                showNutrition(i);
            }

            let ingredient_button = document.createElement("button");
            ingredient_button.innerHTML = "Ingredients";
            ingredient_button.onclick = function(){
                showIngredients(i);
            }

            let recipe_button = document.createElement("button");
            recipe_button.innerHTML = "Recipe";
            recipe_button.onclick = function(){
                showRecipe(i);
            }

            item_container_main.appendChild(title);

            item_container_main.appendChild(item_ingredients);
            item_container_main.appendChild(item_nutrition);
            item_container_main.appendChild(item_recipe);



            item_container_add.appendChild(nutrition_button);
            item_container_add.appendChild(ingredient_button);
            item_container_add.appendChild(recipe_button);
            item_container_add.appendChild(form);
            
            item_container.appendChild(item_container_main);
            item_container.appendChild(item_container_add);
            container.appendChild(item_container);
        }


        //spinner.style.display = "none"
        






    }catch(error){
        console.error(error.message);
        //spinner.style.display = "none"
    }

}





