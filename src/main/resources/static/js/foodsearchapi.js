
const foodList = document.getElementById('item_list');
const searchForm = document.getElementById('searchform');
const searchInput = document.getElementById('search');

const PAGE_SIZE = 12;
let state;

function el(tag, text = '', className = '') {
    const node = document.createElement(tag);
    node.textContent = text;
    node.className = className;
    return node;
}

function asArray(value) {
    return value == null ? [] : Array.isArray(value) ? value : [value];
}

function validImage(url) {
    try {
        const u = new URL(url);
        return u.protocol === 'https:' && (
            u.hostname === 'fatsecret.com' ||
            u.hostname.endsWith('.fatsecret.com') ||
            u.hostname === 'ftscrt.com' ||
            u.hostname.endsWith('.ftscrt.com')
        );
    } catch {
        return false;
    }
}

function makeButton(text, className, action) {
    const button = el('button', text, className);
    button.type = 'button';
    button.onclick = action;
    return button;
}

async function request(path, params, current, signal = current.abort.signal) {
    const response = await fetch(
        `/api/fatsecret/${path}?${new URLSearchParams(params)}`,
        { signal, cache: 'no-store' }
    );

    if (response.redirected && new URL(response.url).pathname === '/login') {
        window.location.assign('/login');
        throw new Error('Please sign in.');
    }

    const data = await response.json();

    if (!response.ok || data.error) {
        throw new Error(
            typeof data.error === 'string'
                ? data.error
                : data.error?.message || 'FatSecret request failed.'
        );
    }

    return data;
}

function getNutrition(data) {
    function number(value) {
        if (value == null || String(value).trim() === '') return null;
        const n = Number(value);
        return Number.isFinite(n) && n >= 0 ? n : null;
    }

    return {
        calories: number(data?.calories),
        protein: number(data?.protein),
        carbs: number(data?.carbohydrate),
        fat: number(data?.fat)
    };
}

function completeNutrition(nutrition) {
    return Object.values(nutrition).every(n => Number.isFinite(n) && n >= 0);
}

function format(value) {
    return Number.isFinite(value) ? Number(value.toFixed(2)) : 'Unavailable';
}

function nutritionPanel(recipe) {
    const n = getNutrition(recipe.recipe_nutrition);
    const panel = el('div', '', 'dish-nutrition');
    const calories = el('div', '', 'dish-calories');
    const macros = el('div', '', 'dish-macros');

    calories.append(
        el('span', 'Per serving', 'nutrition-caption'),
        el('strong', `${format(n.calories)} kcal`)
    );

    for (const [label, value] of [
        ['Protein', n.protein],
        ['Carbs', n.carbs],
        ['Fat', n.fat]
    ]) {
        const item = el('div', '', 'dish-macro');
        item.append(el('strong', `${format(value)} g`), el('span', label));
        macros.append(item);
    }

    panel.append(calories, macros);
    return panel;
}

function addToMealLogger(recipe) {
    const nutrition = getNutrition(recipe.recipe_nutrition);

    if (!completeNutrition(nutrition)) {
        alert('Nutrition information is incomplete.');
        return;
    }

    const selected = {
        name: recipe.recipe_name,
        recipeId: recipe.recipe_id,
        source: 'FatSecret',
        basisLabel: '1 serving',
        ...nutrition
    };

    try {
        sessionStorage.setItem('selectedFood', JSON.stringify(selected));
        window.location.assign('/meals');
    } catch {
        alert('Unable to transfer meal.');
    }
}

function createFoodImage(recipe, className = 'dish-image', lazy = true) {
    const placeholder = () =>
        el('div', 'Photo unavailable', `${className} image-placeholder`);

    if (!validImage(recipe.recipe_image)) return placeholder();

    const image = el('img', '', className);
    image.src = recipe.recipe_image;
    image.alt = recipe.recipe_name;
    image.loading = lazy ? 'lazy' : 'eager';
    image.onerror = () => image.replaceWith(placeholder());

    return image;
}

function renderRecipeInformation(container, recipe) {
    container.replaceChildren();

    const info = el('div', '', 'recipe-info');

    for (const [value, label] of [
        [recipe.number_of_servings, 'Makes'],
        [recipe.preparation_time_min, 'Prep:'],
        [recipe.cooking_time_min, 'Cook:']
    ]) {
        if (value != null) {
            const unit = label === 'Makes' ? 'servings' : 'mins';
            info.append(el('span', `${label} ${value} ${unit}`));
        }
    }

    if (info.children.length) container.append(info);

    container.append(el('h3', 'Ingredients'));

    const ingredients = asArray(recipe.ingredients?.ingredient);
    const ingredientList = el('div', '', 'ingredient-list');

    for (const ingredient of ingredients) {
        const label = el('label', '', 'recipe-ingredient');
        const checkbox = el('input');
        checkbox.type = 'checkbox';

        label.append(
            checkbox,
            el('span', ingredient.ingredient_description ||
                ingredient.food_name || 'Ingredient unavailable')
        );

        ingredientList.append(label);
    }

    container.append(ingredients.length
        ? ingredientList
        : el('p', 'Ingredients unavailable.'));

    container.append(el('h3', 'Cooking Instructions'));

    const directions = asArray(recipe.directions?.direction).slice().sort(
        (a, b) => Number(a.direction_number) - Number(b.direction_number)
    );

    const steps = el('ol', '', 'recipe-steps');

    for (const direction of directions) {
        if (direction.direction_description) {
            steps.append(el('li', direction.direction_description));
        }
    }

    container.append(steps.children.length
        ? steps
        : el('p', 'Cooking instructions unavailable.'));
}

async function openRecipeModal(recipe, current, trigger) {
    if (current !== state || document.querySelector('.recipe-dialog[open]')) {
        return;
    }

    const abort = new AbortController();
    const dialog = el('dialog', '', 'recipe-dialog');
    dialog.setAttribute('aria-labelledby', 'recipe-modal-title');

    const header = el('div', '', 'recipe-modal-header');
    const heading = el('h2', 'Recipe Details');
    heading.id = 'recipe-modal-title';

    const close = makeButton('×', 'recipe-close', () => dialog.close());
    close.setAttribute('aria-label', 'Close recipe');
    header.append(heading, close);

    const content = el('div', '', 'recipe-modal-content');

    content.append(
        createFoodImage(recipe, 'recipe-modal-image', false),
        el('h2', recipe.recipe_name, 'recipe-title')
    );

    if (recipe.recipe_description) {
        content.append(el('p', recipe.recipe_description, 'recipe-description'));
    }

    content.append(nutritionPanel(recipe));

    const details = el('div', 'Loading recipe...', 'recipe-full-details');
    details.setAttribute('aria-live', 'polite');
    content.append(details);

    const footer = el('div', '', 'recipe-modal-footer');

    footer.append(
        makeButton('Close', 'recipe-secondary-button', () => dialog.close()),
        makeButton('+ Add to Meal Logger', 'dish-add-button',
            () => addToMealLogger(recipe))
    );

    dialog.append(header, content, footer);
    document.body.append(dialog);

    dialog.addEventListener('close', () => {
        abort.abort();
        dialog.remove();
        if (trigger.isConnected) trigger.focus();
    });

    dialog.addEventListener('click', event => {
        if (event.target !== dialog) return;

        const rect = dialog.getBoundingClientRect();
        if (event.clientX < rect.left || event.clientX > rect.right ||
            event.clientY < rect.top || event.clientY > rect.bottom) {
            dialog.close();
        }
    });

    dialog.showModal();
    close.focus();

    async function loadDetails() {
        details.textContent = 'Loading recipe...';

        try {
            const result = await request(
                'recipe', { id: recipe.recipe_id }, current, abort.signal
            );

            if (current !== state || !dialog.isConnected || abort.signal.aborted) {
                return;
            }

            if (!result.recipe) throw new Error('Recipe details unavailable.');

            renderRecipeInformation(details, result.recipe);
        } catch (error) {
            if (!dialog.isConnected || abort.signal.aborted) return;

            details.replaceChildren(
                el('p', error.message, 'recipe-error'),
                makeButton('Retry', 'recipe-secondary-button', loadDetails)
            );
        }
    }

    await loadDetails();
}

function recipeCard(recipe, current) {
    const card = el('article', '', 'dish-card');
    const body = el('div', '', 'dish-body');
    const title = el('h3', recipe.recipe_name, 'dish-title');

    title.title = recipe.recipe_name;

    body.append(
        title,
        el('p', recipe.recipe_description || '', 'dish-description'),
        nutritionPanel(recipe)
    );

    const actions = el('div', '', 'dish-actions');

    const viewButton = makeButton('View Recipe', 'dish-view-button',
        () => openRecipeModal(recipe, current, viewButton));

    actions.append(
        makeButton('+ Add to Meal Logger', 'dish-add-button',
            () => addToMealLogger(recipe)),
        viewButton
    );

    body.append(actions);
    card.append(createFoodImage(recipe), body);

    return card;
}

async function loadPage(current) {
    if (current !== state || current.busy) return;

    current.busy = true;
    current.more.disabled = true;
    current.status.textContent = 'Searching dishes...';

    try {
        const result = await request('search', {
            query: current.query,
            page: current.page
        }, current);

        if (current !== state) return;

        const recipes = asArray(result.recipes?.recipe);
        const rawTotal = result.recipes?.total_results;
        const total = rawTotal == null ? NaN : Number(rawTotal);

        for (const recipe of recipes) {
            if (!recipe?.recipe_id || !recipe.recipe_name ||
                !validImage(recipe.recipe_image) ||
                !completeNutrition(getNutrition(recipe.recipe_nutrition))) {
                continue;
            }

            const id = String(recipe.recipe_id);
            if (current.seen.has(id)) continue;

            current.seen.add(id);
            current.grid.append(recipeCard(recipe, current));
        }

        current.page++;

        current.status.textContent = current.seen.size
            ? `${current.seen.size} dishes displayed`
            : 'No dishes with complete nutrition and images found.';

        const hasMore = recipes.length > 0 && (
            Number.isFinite(total)
                ? current.page * PAGE_SIZE < total
                : recipes.length === PAGE_SIZE
        );

        current.more.hidden = !hasMore;
        current.more.textContent = 'Load More';

    } catch (error) {
        if (current !== state) return;

        current.status.textContent = error.message;
        current.more.hidden = false;
        current.more.textContent = 'Retry';

    } finally {
        current.busy = false;
        current.more.disabled = false;
    }
}

async function search(query) {
    state?.abort.abort();
    document.querySelector('.recipe-dialog[open]')?.close();

    const current = state = {
        query,
        page: 0,
        busy: false,
        seen: new Set(),
        abort: new AbortController(),
        status: el('p', '', 'search-status'),
        grid: el('div', '', 'dish-grid'),
        more: el('button', 'Load More', 'load-more-button')
    };

    current.status.setAttribute('role', 'status');
    current.more.type = 'button';
    current.more.hidden = true;
    current.more.onclick = () => loadPage(current);

    foodList.replaceChildren(current.status, current.grid, current.more);
    await loadPage(current);
}

searchForm.addEventListener('submit', event => {
    event.preventDefault();
    const query = searchInput.value.trim();
    if (query) search(query);
});

searchInput.value = 'chicken';
search('chicken');
