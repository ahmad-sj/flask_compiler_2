# A name defined three blocks deep, read after every one of them has closed.
from flask import Flask, render_template

app = Flask(__name__)

rows = [{"id": 1, "tags": ["a", "b"]}]

@app.route('/')
def index():
    for row in rows:
        if row["id"] > 0:
            for tag in row["tags"]:
                while tag != "":
                    innermost = tag
    return render_template('i.jinja', found=innermost)
