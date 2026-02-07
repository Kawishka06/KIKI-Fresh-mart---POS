# 🛒 KIKI FRESH MART – Desktop POS System

A full-featured **desktop Point of Sale (POS) system** built using **JavaFX** and **MySQL**, designed for real-world retail operations.

This project supports secure user authentication, role-based access, inventory management, sales billing, and PDF receipt generation.

> ⭐ This is my **first freelance project**, built end-to-end and deployed for practical usage.

---

## 🚀 Features

- 🔐 Secure Login with **Admin & Cashier roles**
- 👥 Role-based access control  
  - Cashier: Sales only  
  - Admin: Products, Purchases, Reports
- 📦 Product & Inventory Management
- ➕ Stock In / ➖ Stock Out (Loss tracking)
- 🧾 Sales & Billing with dynamic cart
- 📄 **PDF Receipt Generation**
- ⚠️ Low stock detection with visual alerts
- 📊 Reports module (Admin only)
- 🎨 Modern JavaFX UI with collapsible sidebar
- 🔁 Session handling & logout support

---

## 🛠 Tech Stack

- **Java 21**
- **JavaFX**
- **MySQL**
- **JDBC**
- **Apache PDFBox** (PDF receipts)
- **BCrypt** (Password hashing)
- **MVC Architecture**

---

## 🗄 Database

- MySQL relational database
- Tables include:
  - `users`
  - `products`
  - `sales`
  - `sale_items`
  - `purchases`
  - `purchase_items`
  - `stock_movements`

---

## ▶ How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/Kawishka06/kiki-fresh-mart-pos.git

2.Open the project in IntelliJ IDEA

3.Configure MySQL and update DB credentials

4. Run HelloApplication.java

## 🔐 Default Roles (Example)

1. Admin

Full system access

2. Cashier

Sales only (restricted Purchases & Reports)

## 🌟 Highlights

- First real freelance project

- Built with clean architecture

- Focused on usability & real retail workflows

## 📌 Future Improvements

- Barcode scanning

- Sales analytics dashboard

- Supplier management

- Cloud backup support

## 👨‍💻 Author

Kawishka Rathnayake

Computer Science with AI Undergraduate 
