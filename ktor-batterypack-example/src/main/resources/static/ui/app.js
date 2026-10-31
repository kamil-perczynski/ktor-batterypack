document.documentElement.dataset.theme = localStorage.getItem("theme") || "dark";

htmx.config.responseHandling = [
    { code: "204", swap: false },
    { code: "[2345]..", swap: true }
];

const MONTHS = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"];

const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

const sharedTemplates = {
    "tpl-error":
        '<div class="alert alert-danger">' +
        '<div class="alert-body">' +
        '<p class="alert-title">{{data.title}}</p>' +
        "{{#data.detail}}<p>{{data.detail}}</p>{{/data.detail}}" +
        '{{#data.validationJson}}<pre class="code">{{data.validationJson}}</pre>{{/data.validationJson}}' +
        "</div>" +
        "</div>",
    "tpl-nav-auth":
        '<details class="menu">' +
        '<summary class="avatar avatar-sm" title="{{data.auth.email}}">{{data.initial}}</summary>' +
        '<div class="menu-panel">' +
        '<div class="menu-head">' +
        '<p class="strong">{{data.displayName}}</p>' +
        '<p class="muted tiny">{{data.auth.email}}</p>' +
        "</div>" +
        '<a href="/me.html">Profile</a>' +
        '<a class="danger" href="/auth/sign-out">Sign out</a>' +
        "</div>" +
        "</details>",
    "tpl-nav-anon": '<a class="btn btn-primary btn-sm" href="/login.html">Sign in</a>'
};

const viewHelpers = {
    humanize: function () {
        return function (text, render) {
            const value = render(text).trim().replaceAll("_", " ").toLowerCase();
            return value.charAt(0).toUpperCase() + value.slice(1);
        };
    },
    percent: function () {
        return function (text, render) {
            const value = parseFloat(render(text));
            return isNaN(value) ? "0%" : Math.round(value * 100) + "%";
        };
    },
    money: function () {
        return function (text, render) {
            const value = parseFloat(render(text));
            return isNaN(value) ? "—" : value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        };
    },
    date: function () {
        return function (text, render) {
            const value = render(text).trim();
            if (!value) {
                return "—";
            }
            const numeric = Number(value);
            const parsed = isNaN(numeric) ? new Date(value) : new Date(numeric * 1000);
            return parsed.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
        };
    }
};

function escapeHtml(text) {
    return String(text)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function parseJson(text) {
    try {
        return text ? JSON.parse(text) : null;
    } catch (e) {
        return { title: "Unexpected response", detail: text };
    }
}

function scheduleRow(label, schedule) {
    return {
        label: label,
        values: MONTHS.map(function (month) {
            const days = schedule ? schedule[month] : 0;
            return days > 0 ? days + "d" : "—";
        })
    };
}

function prepareAuth(info) {
    if (!info) {
        return info;
    }
    const auth = info.auth || {};
    info.displayName = auth.preferredUsername || auth.username || auth.email || "Account";
    info.initial = (auth.email || info.displayName).charAt(0);
    return info;
}

function preparePlant(plant) {
    if (!plant) {
        return plant;
    }
    const top = (plant.identifications || [])[0] || {};
    const care = plant.careInstructions || {};

    plant.name = plant.displayName || top.mostLikelyCommonName || plant.species || top.species || "Unnamed plant";
    plant.subtitle = plant.species || top.species || "";
    plant.variety = plant.cultivar || top.cultivar || "";
    plant.cover = (plant.images || [])[0] ? plant.images[0].url : null;
    plant.healthy = plant.healthStatus && plant.healthStatus.condition === "HEALTHY";
    plant.toxic = plant.toxicity && plant.toxicity.level !== "NONE";
    plant.months = MONTH_NAMES;
    plant.schedule = [
        scheduleRow("Watering", care.watering),
        scheduleRow("Fertilizing", care.fertilizing)
    ];
    plant.repottingMonths = care.repotting
        ? (care.repotting.recommendedMonths || []).map(function (m) { return MONTH_NAMES[m - 1]; }).join(", ")
        : "";
    return plant;
}

const preparers = {
    auth: prepareAuth,
    plant: preparePlant,
    plants: function (listing) {
        (listing.items || []).forEach(preparePlant);
        return listing;
    }
};

function renderTemplate(templateId, data) {
    const template = document.getElementById(templateId);
    const source = template ? template.innerHTML : sharedTemplates[templateId];
    if (!source) {
        return "<p>Missing template: " + escapeHtml(templateId) + "</p>";
    }
    return Mustache.render(source, Object.assign({}, viewHelpers, { data: data }));
}

function renderRaw(data, xhr) {
    const color = xhr.status < 400 ? "badge-success" : "badge-danger";
    return '<span class="badge badge-dot ' + color + '">' + xhr.status + " " + escapeHtml(xhr.statusText) + "</span>" +
        '<pre class="code">' + escapeHtml(JSON.stringify(data, null, 2)) + "</pre>";
}

htmx.defineExtension("json-view", {
    onEvent: function (name, event) {
        if (name === "htmx:configRequest" && event.detail.elt.hasAttribute("data-json-body")) {
            event.detail.headers["Content-Type"] = "application/json";
        }
    },
    encodeParameters: function (xhr, parameters, elt) {
        if (!elt.hasAttribute("data-json-body")) {
            return null;
        }
        const body = {};
        parameters.forEach(function (value, key) {
            const input = elt.querySelector('[name="' + key + '"]');
            body[key] = input && input.type === "number" && value !== "" ? Number(value) : value;
        });
        xhr.overrideMimeType("text/json");
        return JSON.stringify(body);
    },
    transformResponse: function (text, xhr, elt) {
        const data = parseJson(text);

        if (elt.dataset.view === "raw") {
            return renderRaw(data, xhr);
        }

        if (xhr.status >= 400) {
            if (data && data.validation) {
                data.validationJson = JSON.stringify(data.validation, null, 2);
            }
            return renderTemplate(elt.dataset.errorTemplate || "tpl-error", data || { title: xhr.statusText });
        }

        const prepare = preparers[elt.dataset.prepare];
        return renderTemplate(elt.dataset.template, prepare ? prepare(data) : data);
    }
});

function loadingButtons(elt) {
    if (elt.matches("button, .btn")) {
        return [elt];
    }
    return Array.from(elt.querySelectorAll("button[type=submit]"));
}

document.addEventListener("htmx:beforeRequest", function (event) {
    loadingButtons(event.detail.elt).forEach(function (button) {
        if (!button.querySelector(".spinner")) {
            button.insertAdjacentHTML("afterbegin", '<span class="spinner"></span>');
        }
    });
});

document.addEventListener("htmx:afterRequest", function (event) {
    loadingButtons(event.detail.elt).forEach(function (button) {
        button.querySelectorAll(".spinner").forEach(function (spinner) {
            spinner.remove();
        });
    });
});

document.addEventListener("click", function (event) {
    document.querySelectorAll("details.menu[open]").forEach(function (menu) {
        if (!menu.contains(event.target)) {
            menu.removeAttribute("open");
        }
    });
});

htmx.onLoad(function (content) {
    if (!content.querySelectorAll) {
        return;
    }
    content.querySelectorAll("a[data-nav]").forEach(function (link) {
        if (link.getAttribute("href") === window.location.pathname) {
            link.classList.add("active");
        }
    });
});

function toggleTheme() {
    const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = next;
    localStorage.setItem("theme", next);
}

function queryParam(name) {
    return new URLSearchParams(window.location.search).get(name);
}
