function insertThrobber(container) {
  var r = Raphael(container, 600, 300),
  sectorsCount = 12,
  color = "#000",
  width = 15,
  r1 = 35,
  r2 = 60,
  cx = 300,
  cy = 300,

  sectors = [],
  opacity = [],
  beta = 2 * Math.PI / sectorsCount,

  pathParams = {stroke: color, "stroke-width": width, "stroke-linecap": "round"};
  for (var i = 0; i < sectorsCount; i++) {
    var alpha = beta * i - Math.PI / 2,
    cos = Math.cos(alpha),
    sin = Math.sin(alpha);
    opacity[i] = 1 / sectorsCount * i;
    sectors[i] = r.path(pathParams)//.attr("stroke", Raphael.getColor())
    .moveTo(cx + r1 * cos, cy + r1 * sin)
    .lineTo(cx + r2 * cos, cy + r2 * sin);
  }
  (function ticker() {
    opacity.unshift(opacity.pop());
    for (var i = 0; i < sectorsCount; i++) {
      sectors[i].attr("opacity", opacity[i]);
    }
    r.safari();
    setTimeout(ticker, 1000 / sectorsCount);
  })();
};
