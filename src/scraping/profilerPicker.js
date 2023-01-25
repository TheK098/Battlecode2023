function pickId(id) {
    const opts = document.querySelectorAll("option[value]");
    for (opt of opts) {
        if (opt.text.includes(id)) opt.parentElement.selectedIndex = opt.value;
    }
}