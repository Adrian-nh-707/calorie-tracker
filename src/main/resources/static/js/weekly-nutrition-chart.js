/**
 * Fix weekly nutrition chart tooltip overflow.
 * When a tooltip would extend past the right edge of the viewport,
 * flip it to align from the right edge instead.
 */
(function () {
  document.querySelectorAll('.weekly-bar-link').forEach(function (link) {
    var tooltip = link.querySelector(':scope > .weekly-tooltip');
    if (!tooltip) return;

    link.addEventListener('mouseenter', function () {
      var rect = tooltip.getBoundingClientRect();
      if (rect.right > window.innerWidth) {
        tooltip.classList.add('tooltip-right-aligned');
      }
    });

    link.addEventListener('mouseleave', function () {
      tooltip.classList.remove('tooltip-right-aligned');
    });
  });
})();
