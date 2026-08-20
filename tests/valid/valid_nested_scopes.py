# Deeply nested but entirely legal scoping: every name is read inside the
# block that defines it, or from an enclosing scope.
from flask import Flask, render_template

app = Flask(__name__)

rows = [{"id": 1, "qty": 3, "price": 5}, {"id": 2, "qty": 0, "price": 9}]

@app.route('/')
def index():
    grand = 0
    for row in rows:
        line = row["qty"] * row["price"]
        if line > 0:
            adjusted = line
            while adjusted > 100:
                adjusted = adjusted - 100
            grand = grand + adjusted
        else:
            grand = grand + 0
    return render_template('i.jinja', grand=grand, rows=rows)
