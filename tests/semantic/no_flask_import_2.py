import os
app = Flask(__name__)

@app.route('/a')
def a():
    x = 1
    return render_template('i.jinja')
