
from flask import Flask, render_template, request, redirect, url_for

app = Flask(__name__)

# In-memory list of products (id, name, price, details, image_url)
products = [
    {"id": 1, "name": "Wireless Headphones", "price": 79.99,
     "details": "Noise-cancelling over-ear headphones with 30-hour battery life.",
     "image": "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400"},

    {"id": 2, "name": "Mechanical Keyboard", "price": 129.99,
     "details": "RGB backlit mechanical keyboard with blue switches.",
     "image": "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=400"},

    {"id": 3, "name": "Smartwatch", "price": 249.99,
     "details": "Fitness tracking, heart rate monitor, and GPS.",
     "image": "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400"},
]

# Helper to get the next id
def next_id():
    return max(p["id"] for p in products) + 1 if products else 1

@app.route('/')
def index():
    return render_template('index.html', products=products)

@app.route('/product/<int:product_id>')
def detail(product_id):
    product = next((p for p in products if p["id"] == product_id), None)
    if not product:
        return "Product not found", 404
    return render_template('detail.html', product=product)

@app.route('/add', methods=['GET', 'POST'])
def add():
    if request.method == 'POST':
        # Gather form data
        name = request.form['name']
        price = float(request.form['price'])
        details = request.form['details']
        image = request.form['image'] or "https://via.placeholder.com/400x300?text=No+Image"

        products.append({
            "id": next_id(),
            "name": name,
            "price": price,
            "details": details,
            "image": image
        })
        return redirect(url_for('index'))

    return render_template('add.html')

if __name__ == '__main__':
    app.run(debug=True)