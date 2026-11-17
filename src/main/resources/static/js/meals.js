
document.addEventListener('DOMContentLoaded', () => {
    const $ = id => document.getElementById(id);

    const form = $('mealForm');
    const nameInput = $('name');
    const recipeIdInput = $('recipeId');
    const preview = $('mealPreview');
    const previewPhoto = $('previewPhoto');
    const portionControls = $('portionControls');
    const portionAmount = $('portionAmount');
    const message = $('foodImportMessage');

    const fields = ['calories', 'protein', 'carbs', 'fat'];
    const imageRequests = new Map();

    let importedMeal = null;

    function validRecipeId(value) {
        const id = String(value ?? '');
        return /^[0-9]{1,19}$/.test(id) && /[1-9]/.test(id);
    }

    function validImage(url) {
        if (typeof url !== 'string') return false;

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

    function fetchRecipeImage(recipeId) {
        const id = String(recipeId);

        if (!validRecipeId(id)) return Promise.resolve(null);
        if (imageRequests.has(id)) return imageRequests.get(id);

        const request = (async () => {
            const response = await fetch(
                `/api/fatsecret/recipe?${new URLSearchParams({ id })}`,
                { cache: 'no-store' }
            );

            if (!response.ok || response.redirected) return null;

            const data = await response.json();
            if (data.error) return null;

            const images = data.recipe?.recipe_images?.recipe_image;
            const list = Array.isArray(images) ? images : images ? [images] : [];

            return list.find(validImage) || null;

        })().catch(() => null);

        imageRequests.set(id, request);
        return request;
    }

    function resetPhoto(container) {
        const image = container.querySelector('img');
        const placeholder = container.querySelector('.meal-photo-placeholder');

        container.dataset.loadingId = '';
        image.onload = null;
        image.onerror = null;
        image.removeAttribute('src');
        image.alt = '';
        image.hidden = true;
        placeholder.hidden = false;
    }

    async function displayPhoto(container, recipeId, mealName) {
        resetPhoto(container);

        if (!validRecipeId(recipeId)) return;

        const id = String(recipeId);
        container.dataset.loadingId = id;

        const image = container.querySelector('img');
        const placeholder = container.querySelector('.meal-photo-placeholder');

        const url = await fetchRecipeImage(id);

        const isCurrent = () =>
            container.dataset.loadingId === id && container.isConnected;

        if (!isCurrent() || !validImage(url)) return;

        image.onload = () => {
            if (!isCurrent()) return;
            image.hidden = false;
            placeholder.hidden = true;
        };

        image.onerror = () => {
            if (!isCurrent()) return;
            image.hidden = true;
            placeholder.hidden = false;
        };

        image.alt = mealName || 'Meal photo';
        image.src = url;
    }

    function loadHistoryPhoto(container) {
        displayPhoto(
            container,
            container.dataset.recipeId,
            container.dataset.mealName
        );
    }

    const historyPhotos = document.querySelectorAll('.history-photo[data-recipe-id]');

    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver(entries => {
            for (const entry of entries) {
                if (entry.isIntersecting) {
                    loadHistoryPhoto(entry.target);
                    observer.unobserve(entry.target);
                }
            }
        }, { rootMargin: '200px' });

        historyPhotos.forEach(container => {
            if (validRecipeId(container.dataset.recipeId)) {
                observer.observe(container);
            }
        });
    } else {
        historyPhotos.forEach(loadHistoryPhoto);
    }

    function validNutrition(value) {
        return value != null &&
            String(value).trim() !== '' &&
            Number.isFinite(Number(value)) &&
            Number(value) >= 0;
    }

    function format(value) {
        return String(Number(value.toFixed(2)));
    }

    function updateNutrition() {
        if (!importedMeal) return;

        const amount = portionAmount.valueAsNumber;

        if (!Number.isFinite(amount) || amount < 0.01) {
            portionAmount.setCustomValidity(
                'Please enter at least 0.01 servings.'
            );
            return;
        }

        const totals = fields.map(field => importedMeal[field] * amount);

        if (!totals.every(Number.isFinite)) {
            portionAmount.setCustomValidity('Please enter a smaller serving amount.');
            return;
        }

        portionAmount.setCustomValidity('');

        fields.forEach((field, i) => {
            $(field).value = format(totals[i]);
        });

        $('previewCalories').textContent = format(totals[0]);

        document.querySelectorAll('[data-portion]').forEach(button => {
            const active = Number(button.dataset.portion) === amount;
            button.classList.toggle('portion-active', active);
            button.setAttribute('aria-pressed', String(active));
        });
    }

    function clearImport() {
        importedMeal = null;
        recipeIdInput.value = '';

        preview.hidden = true;
        resetPhoto(previewPhoto);

        message.hidden = true;
        message.textContent = '';

        portionControls.hidden = true;
        portionAmount.disabled = true;
        portionAmount.setCustomValidity('');
    }

    function importFood() {
        let selected;

        try {
            const raw = sessionStorage.getItem('selectedFood');
            if (!raw) return;

            sessionStorage.removeItem('selectedFood');
            selected = JSON.parse(raw);
        } catch {
            return;
        }

        if (!selected ||
            typeof selected.name !== 'string' ||
            !selected.name.trim() ||
            !fields.every(field => validNutrition(selected[field]))) {
            return;
        }

        importedMeal = {
            name: selected.name,
            recipeId: selected.source === 'FatSecret' &&
                validRecipeId(selected.recipeId)
                ? String(selected.recipeId)
                : ''
        };

        fields.forEach(field => {
            importedMeal[field] = Number(selected[field]);
        });

        nameInput.value = importedMeal.name;
        recipeIdInput.value = importedMeal.recipeId;

        preview.hidden = false;
        $('previewName').textContent = importedMeal.name;

        if (importedMeal.recipeId) {
            displayPhoto(previewPhoto, importedMeal.recipeId, importedMeal.name);
        }

        portionControls.hidden = false;
        portionAmount.disabled = false;
        portionAmount.value = '1';

        $('portionBasis').textContent = selected.basisLabel || '1 serving';

        message.hidden = false;
        message.textContent =
            'Meal imported successfully. Choose your serving amount and meal type.';

        updateNutrition();
    }

    portionAmount.addEventListener('input', updateNutrition);

    document.querySelectorAll('[data-portion]').forEach(button => {
        button.addEventListener('click', () => {
            portionAmount.value = button.dataset.portion;
            updateNutrition();
        });
    });

    $('clearImport').addEventListener('click', clearImport);

    nameInput.addEventListener('input', () => {
        if (importedMeal && nameInput.value !== importedMeal.name) {
            clearImport();
        }
    });

    $('calories').addEventListener('input', () => {
        if (importedMeal && validNutrition($('calories').value)) {
            $('previewCalories').textContent = $('calories').value;
        }
    });

    form.addEventListener('submit', event => {
        if (importedMeal && !portionAmount.checkValidity()) {
            event.preventDefault();
            portionAmount.reportValidity();
        }
    });

    importFood();
});
