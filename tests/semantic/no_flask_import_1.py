app = Flask(__name__)

@app.route('/')
def index():
    x = 1
    return render_template('i.jinja')
