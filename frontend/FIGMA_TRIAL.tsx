// Paste your trial frontend code here for analysis.
// Once you paste the code, I will review it for:
// - Functionality coverage vs FIGMA_DESIGN_PROMPT.md specs
// - Component structure and reusability
// - API integration completeness
// - Missing features or screens
// - UX/interaction pattern alignment

import React, { useMemo, useState } from "react";
import {
  LayoutDashboard,
  ShoppingCart,
  CreditCard,
  Factory,
  HardHat,
  BookOpen,
  Package,
  Building2,
  Warehouse,
  Star,
  BarChart3,
  MessageCircle,
  Sparkles,
  TrendingUp,
  MapPin,
  ArrowLeftRight,
  Bell,
  Search,
  Filter,
  Plus,
  Trash2,
  Check,
  AlertTriangle,
  User,
  LogOut,
  Globe,
  ChevronRight,
  ClipboardList,
  Shield,
  ChefHat,
  Clock3,
  CircleDollarSign,
  Send,
} from "lucide-react";

const colors = {
  primary: "#2563EB",
  primaryHover: "#1D4ED8",
  primaryTint: "#EFF6FF",
  gray50: "#F9FAFB",
  gray100: "#F3F4F6",
  gray200: "#E5E7EB",
  gray500: "#6B7280",
  gray700: "#374151",
  gray900: "#111827",
  slate900: "#0F172A",
  slate800: "#1E293B",
  green600: "#16A34A",
  green100: "#DCFCE7",
  red600: "#DC2626",
  red100: "#FEE2E2",
  yellow600: "#CA8A04",
  yellow100: "#FEF9C3",
  blue100: "#DBEAFE",
  amber100: "#FEF3C7",
  purple100: "#F3E8FF",
  orange100: "#FFEDD5",
  teal100: "#CCFBF1",
};

const translations = {
  en: {
    dashboard: "Dashboard",
    orders: "Orders",
    pos: "POS",
    productionPlans: "Production Plans",
    floor: "Floor View",
    recipes: "Recipes",
    products: "Products",
    departments: "Departments",
    inventory: "Inventory",
    reports: "Reports",
    customers: "Customers",
    loyalty: "Loyalty",
    aiWhatsapp: "AI WhatsApp",
    aiSuggestions: "AI Suggestions",
    aiPricing: "AI Pricing",
    exchangeRates: "Exchange Rates",
    subscriptions: "Subscriptions",
    login: "Sign In",
    customerPortal: "Customer Portal",
    bakeryManagement: "Bakery Management System",
    revenueToday: "Revenue Today",
    openOrders: "Open Orders",
    plansToday: "Plans Today",
    stockValue: "Stock Value",
    allConversations: "All Conversations",
    escalated: "Escalated",
    ourProducts: "Our Products",
    placeOrder: "Place Order",
  },
  hy: {
    dashboard: "Վահանակ",
    orders: "Պատվերներ",
    pos: "POS",
    productionPlans: "Արտադրության պլաններ",
    floor: "Արտադրամաս",
    recipes: "Բաղադրատոմսեր",
    products: "Ապրանքներ",
    departments: "Բաժիններ",
    inventory: "Պահեստ",
    reports: "Հաշվետվություններ",
    customers: "Հաճախորդներ",
    loyalty: "Հավատարմություն",
    aiWhatsapp: "AI WhatsApp",
    aiSuggestions: "AI առաջարկներ",
    aiPricing: "AI գնագոյացում",
    exchangeRates: "Փոխարժեքներ",
    subscriptions: "Բաժանորդագրություններ",
    login: "Մուտք գործել",
    customerPortal: "Հաճախորդի պորտալ",
    bakeryManagement: "Հացի արտադրության կառավարման համակարգ",
    revenueToday: "Այսօրվա եկամուտ",
    openOrders: "Բաց պատվերներ",
    plansToday: "Այսօրվա պլաններ",
    stockValue: "Պահեստի արժեք",
    allConversations: "Բոլոր զրույցները",
    escalated: "Էսկալացված",
    ourProducts: "Մեր ապրանքները",
    placeOrder: "Պատվիրել",
  },
};

const statusStyles = {
  DRAFT: "bg-gray-100 text-gray-700",
  ACTIVE: "bg-green-100 text-green-800",
  CONFIRMED: "bg-blue-100 text-blue-800",
  IN_PRODUCTION: "bg-amber-100 text-amber-800",
  IN_PROGRESS: "bg-amber-100 text-amber-800",
  COMPLETED: "bg-green-100 text-green-800",
  CANCELLED: "bg-red-100 text-red-800",
  READY: "bg-teal-100 text-teal-800",
  DELIVERED: "bg-green-100 text-green-800",
  OUT_FOR_DELIVERY: "bg-blue-100 text-blue-800",
  PUBLISHED: "bg-teal-100 text-teal-800",
  APPROVED: "bg-purple-100 text-purple-800",
  GENERATED: "bg-orange-100 text-orange-800",
  OVERDUE: "bg-red-100 text-red-800",
  PENDING: "bg-yellow-100 text-yellow-800",
  NOT_STARTED: "bg-gray-100 text-gray-700",
};

const hyTextMap = {
  "Design prototype built from your uploaded system spec": "Դիզայնի պրոտոտիպը կառուցված է ձեր ներբեռնած համակարգի սպեցիֆիկացիայի հիման վրա",
  "ERP / design shell": "ERP / դիզայնի կառուցվածք",
  "Back-Office": "Հետին համակարգ",
  "Customer Portal": "Հաճախորդի պորտալ",
  "Login": "Մուտք",
  "Alerts": "Ծանուցումներ",
  "Admin": "Ադմին",
  "Out": "Ելք",
  "Overview": "Ընդհանուր պատկերը",
  "Spec Coverage": "Սպեցիֆիկացիայի ծածկույթ",
  "Foundations": "Հիմքեր",
  "App Shell": "Հավելվածի կառուցվածք",
  "Core Operations": "Հիմնական օպերացիաներ",
  "AI Screens": "AI էկրաններ",
  "Portal Preview": "Պորտալի նախադիտում",
  "Real-time operations snapshot for sales, production, inventory, and delivery.": "Իրական ժամանակի օպերացիոն պատկերը վաճառքի, արտադրության, պահեստի և առաքման համար։",
  "Quick Action": "Արագ գործողություն",
  "+12.4% vs yesterday": "+12.4% երեկվա համեմատ",
  "3 require production start": "3-ը պահանջում են արտադրության մեկնարկ",
  "2 published, 1 in progress": "2-ը հրապարակված են, 1-ը ընթացքի մեջ",
  "4 low-stock alerts": "4 ցածր պաշարի ահազանգ",
  "Delivery Timeline": "Առաքման ժամանակացույց",
  "Live": "Ուղիղ",
  "Run 01": "Ռան 01",
  "Run 02": "Ռան 02",
  "Run 03": "Ռան 03",
  "Delivered": "Առաքված",
  "Out for delivery": "Առաքման մեջ",
  "Ready": "Պատրաստ",
  "Stock Alerts": "Պահեստային ահազանգեր",
  "Below reorder point": "Վերապատվիրման կետից ցածր",
  "Critical": "Կրիտիկական",
  "3 days supply remaining": "Մնացել է 3 օրվա պաշար",
  "Warning": "Զգուշացում",
  "Needs restock": "Պետք է վերալիցքավորվի",
  "Used faster than forecast": "Օգտագործվել է կանխատեսվածից արագ",
  "Info": "Տեղեկանք",
  "Production Floor": "Արտադրամասի առաջընթաց",
  "Bread": "Հաց",
  "Pastry": "Խմորեղեն",
  "Lavash": "Լավաշ",
  "work orders complete": "աշխատանքային պատվեր ավարտված",
  "Issues Detector": "Խնդիրների հայտնաբերում",
  "Review All": "Դիտել բոլորը",
  "Orders without plans": "Պատվերներ առանց պլանի",
  "2 wholesale orders need scheduling": "2 մեծածախ պատվեր պահանջում է պլանավորում",
  "Plans without active recipe": "Պլաններ առանց ակտիվ բաղադրատոմսի",
  "Croissant v3 draft exists but is not active": "Croissant v3 սևագիրը կա, բայց ակտիվ չէ",
  "Low stock items": "Ցածր պաշարի ապրանքներ",
  "Flour and butter could block tomorrow morning production": "Ալյուրն ու կարագը կարող են արգելափակել վաղվա առավոտյան արտադրությունը",
  "Getting Started Wizard": "Սկսելու ուղեցույց",
  "Create department": "Ստեղծել բաժին",
  "Add products": "Ավելացնել ապրանքներ",
  "Add recipes": "Ավելացնել բաղադրատոմսեր",
  "Configure inventory": "Կարգավորել պահեստը",
  "Sales": "Վաճառք",
  "Orders": "Պատվերներ",
  "Manage the full order lifecycle from draft to delivery.": "Կառավարեք պատվերի ամբողջ ցիկլը սևագրից մինչև առաքում։",
  "New Order": "Նոր պատվեր",
  "Customer Search": "Հաճախորդի որոնում",
  "Search by name or phone": "Որոնել անունով կամ հեռախոսահամարով",
  "Status": "Կարգավիճակ",
  "Date Range": "Ամսաթվերի միջակայք",
  "Filters": "Ֆիլտրեր",
  "Order #": "Պատվեր #",
  "Customer": "Հաճախորդ",
  "Phone": "Հեռախոս",
  "Order Date": "Պատվերի ամսաթիվ",
  "Delivery Date": "Առաքման ամսաթիվ",
  "Total": "Ընդամենը",
  "Actions": "Գործողություններ",
  "Start": "Սկսել",
  "Cancel": "Չեղարկել",
  "Edit": "Խմբագրել",
  "Notes": "Նշումներ",
  "Create Order Modal Preview": "Պատվերի ստեղծման պատուհանի նախադիտում",
  "Customer Name": "Հաճախորդի անուն",
  "Rush Delivery": "Շտապ առաքում",
  "Apply premium": "Կիրառել հավելավճար",
  "Product": "Ապրանք",
  "Qty": "Քանակ",
  "Unit Price": "Մեկ միավորի գին",
  "Add Line": "Ավելացնել տող",
  "Order Total": "Պատվերի գումար",
  "Status Flow": "Կարգավիճակի հոսք",
  "Confirm and production start actions are prominent for managers and production users.": "Հաստատման և արտադրության մեկնարկի գործողությունները ընդգծված են մենեջերների և արտադրական օգտատերերի համար։",
  "Rush orders surface clearly in filters and in row-level actions.": "Շտապ պատվերները հստակ երևում են ֆիլտրերում և տողային գործողություններում։",
  "Cancel requires a reason modal to preserve auditability.": "Չեղարկումը պահանջում է պատճառի պատուհան, որպեսզի պահպանվի հետագծելիությունը։",
  "Retail": "Մանրածախ",
  "Point of Sale": "Վաճառքի կետ",
  "Fast-touch cash desk flow with product grid, cart, and payment states.": "Արագ դրամարկղային հոսք ապրանքների ցանցով, զամբյուղով և վճարման կարգավիճակներով։",
  "Bakery Front": "Վաճառասրահ",
  "Product Grid": "Ապրանքների ցանց",
  "Cart": "Զամբյուղ",
  "Subtotal": "Ենթագումար",
  "VAT": "ԱԱՀ",
  "Cash": "Կանխիկ",
  "Card": "Քարտ",
  "Payment Modal Preview": "Վճարման պատուհանի նախադիտում",
  "Amount Tendered": "Տրված գումար",
  "Change": "Մնացորդ",
  "Receipt preview, transaction number, business name, itemized totals, and print state live here.": "Այստեղ երևում են կտրոնի նախադիտումը, գործարքի համարը, բիզնեսի անունը, մանրամասն գումարները և տպման կարգավիճակը։",
  "End of Day Reconciliation": "Օրվա վերջի համադրում",
  "Cash expected": "Սպասվող կանխիկ",
  "Cash counted": "Հաշվված կանխիկ",
  "Card subtotal": "Քարտային ենթագումար",
  "Overage / shortage": "Ավելցուկ / պակասորդ",
  "Production Plans": "Արտադրական պլաններ",
  "Plan shifts, generate work orders, and visualize production timing.": "Պլանավորեք հերթափոխերը, ստեղծեք աշխատանքային պատվերներ և տեսանելի դարձրեք արտադրության ժամանակացույցը։",
  "New Plan": "Նոր պլան",
  "Plan": "Պլան",
  "Date": "Ամսաթիվ",
  "Shift": "Հերթափոխ",
  "Department": "Բաժին",
  "Work Orders": "Աշխատանքային պատվերներ",
  "Morning Batch — March 10": "Առավոտյան խմբաքանակ — Մարտ 10",
  "Morning": "Առավոտյան",
  "Afternoon": "Կեսօրից հետո",
  "Night": "Գիշերային",
  "Publish": "Հրապարակել",
  "Approve": "Հաստատել",
  "Plan Detail": "Պլանի մանրամասներ",
  "Scheduled": "Պլանավորված",
  "Material Requirements": "Նյութերի պահանջարկ",
  "Gantt Schedule View": "Գանտի ժամանակացույց",
  "Floor View": "Արտադրամաս",
  "Tablet-friendly daily execution for workers and shift leads.": "Պլանշետին հարմար ամենօրյա աշխատանքային տեսք աշխատողների և հերթափոխի ղեկավարների համար։",
  "Selected Work Order": "Ընտրված աշխատանքային պատվեր",
  "Technology Steps": "Տեխնոլոգիական քայլեր",
  "Recipe": "Բաղադրատոմս",
  "Mix dough": "Խառնել խմորը",
  "First proof": "Առաջին խմորում",
  "Shape loaves": "Ձևավորել հացերը",
  "Second proof": "Երկրորդ խմորում",
  "Bake at 220°C": "Թխել 220°C-ում",
  "Recipe Snapshot": "Բաղադրատոմսի ամփոփում",
  "Complete": "Ավարտել",
  "Inventory": "Պահեստ",
  "FIFO lots, stock alerts, receiving, transfers, and adjustments.": "FIFO խմբաքանակներ, պաշարի ահազանգեր, ստացում, տեղափոխումներ և ճշգրտումներ։",
  "Stock Levels": "Պաշարի մակարդակներ",
  "Items": "Ապրանքներ",
  "Item": "Ապրանք",
  "Location": "Տեղակայություն",
  "Current Qty": "Ընթացիկ քանակ",
  "Lot #": "Խմբաքանակ #",
  "Reorder": "Վերապատվիրել",
  "LOW": "Ցածր",
  "OK": "Լավ",
  "Alerts & Actions": "Ահազանգեր և գործողություններ",
  "Receive Stock": "Ստանալ պաշար",
  "Receive item, lot, supplier, currency, unit cost": "Ստացեք ապրանք, խմբաքանակ, մատակարար, արժույթ և միավորի ինքնարժեք",
  "Transfer": "Տեղափոխում",
  "Move inventory between locations": "Տեղափոխել պաշարը տեղակայությունների միջև",
  "Adjust": "Ճշգրտում",
  "Waste, spoilage, count correction": "Կորուստ, փչացում, հաշվարկի ճշգրտում",
  "Flour is below reorder point and could block tomorrow morning production.": "Ալյուրը վերապատվիրման կետից ցածր է և կարող է արգելափակել վաղվա առավոտյան արտադրությունը։",
  "Receive Stock Modal": "Պաշարի ստացման պատուհան",
  "Quantity": "Քանակ",
  "Currency / FX": "Արժույթ / Փոխարժեք",
  "Supplier": "Մատակարար",
  "Transfer Modal": "Տեղափոխման պատուհան",
  "From Location": "Որտեղից",
  "To Location": "Որտեղ",
  "Adjust Modal": "Ճշգրտման պատուհան",
  "Qty (+/-)": "Քանակ (+/-)",
  "Reason": "Պատճառ",
  "AI Tools": "AI գործիքներ",
  "AI WhatsApp Conversations": "AI WhatsApp զրույցներ",
  "Monitor AI order intake, escalations, and draft-order conversion.": "Հետևեք AI պատվերների ընդունմանը, էսկալացիաներին և սևագիր պատվերների փոխակերպմանը։",
  "Conversation List": "Զրույցների ցանկ",
  "Chat Detail": "Զրույցի մանրամասներ",
  "Draft Order": "Սևագիր պատվեր",
  "Delivery": "Առաքում",
  "Human escalation is visible, with explicit action to resolve and convert the conversation into an operational order.": "Մարդու էսկալացիան տեսանելի է, հստակ գործողությամբ այն լուծելու և զրույցը օպերացիոն պատվերի վերածելու համար։",
  "Resolve Escalation": "Լուծել էսկալացիան",
  "Hello! I can help you place an order.": "Բարև։ Կարող եմ օգնել ձեզ պատվեր ձևակերպել։",
  "I've noted your order and prepared a draft for confirmation.": "Նշել եմ ձեր պատվերը և պատրաստել եմ սևագիր հաստատման համար։",
  "AI Suggestions": "AI առաջարկներ",
  "Replenishment, demand forecasting, and production recommendations.": "Լրացման, պահանջարկի կանխատեսման և արտադրության առաջարկություններ։",
  "Replenishment": "Լրացման առաջարկներ",
  "Confidence": "Վստահություն",
  "Current stock:": "Ընթացիկ պաշար.",
  "Recommended:": "Առաջարկվում է.",
  "Create PO": "Ստեղծել գնման պատվեր",
  "Dismiss": "Մերժել",
  "Demand Forecast": "Պահանջարկի կանխատեսում",
  "Historical vs predicted demand": "Պատմական ընդդեմ կանխատեսված պահանջարկի",
  "Production Suggestions": "Արտադրության առաջարկներ",
  "Open orders + forecast - current stock": "Բաց պատվերներ + կանխատեսում - ընթացիկ պաշար",
  "Create Plan": "Ստեղծել պլան",
  "AI Pricing & Anomalies": "AI գնագոյացում և անոմալիաներ",
  "Price optimization and anomaly surfacing for management and finance.": "Գնի օպտիմալացում և անոմալիաների ցուցադրում կառավարման և ֆինանսների համար։",
  "Pricing Suggestions": "Գնային առաջարկներ",
  "Current": "Ընթացիկ",
  "Suggested": "Առաջարկվող",
  "Change": "Փոփոխություն",
  "Anomalies": "Անոմալիաներ",
  "Cost spike": "Ինքնարժեքի թռիչք",
  "Revenue dip": "Եկամտի անկում",
  "Margin erosion": "Մարժայի նվազում",
  "Our Products": "Մեր ապրանքները",
  "Separate customer-facing app with catalog, ordering, loyalty, and account settings.": "Առանձին հաճախորդակենտրոն հավելված կատալոգով, պատվերով, հավատարմությամբ և հաշվի կարգավորումներով։",
  "Search": "Որոնում",
  "Category": "Կատեգորիա",
  "Sort": "Դասավորում",
  "All": "Բոլորը",
  "Popular": "Հանրահայտ",
  "Add to cart": "Ավելացնել զամբյուղ",
  "Shopping Cart & Checkout": "Զամբյուղ և վճարում",
  "Customer-specific pricing, rush toggle, delivery date, and loyalty redemption.": "Հաճախորդին հատուկ գներ, շտապ առաքում, առաքման ամսաթիվ և հավատարմության օգտագործում։",
  "Your Cart": "Ձեր զամբյուղը",
  "Summary": "Ամփոփում",
  "Special Notes": "Հատուկ նշումներ",
  "+15% rush premium": "+15% շտապ հավելավճար",
  "Delivery Address": "Առաքման հասցե",
  "Discount": "Զեղչ",
  "Rush": "Շտապ",
  "Place Order": "Պատվիրել",
  "My Loyalty": "Իմ հավատարմությունը",
  "Tier status, points history, and redemption patterns.": "Կարգավիճակ, միավորների պատմություն և օգտագործման ձևաչափեր։",
  "Tier Status": "Կարգավիճակ",
  "Silver Member": "Արծաթե անդամ",
  "Points toward Gold": "Միավոր մինչև Gold",
  "Discount 5% • Free delivery on orders over ֏50K": "Զեղչ 5% • Անվճար առաքում ֏50K-ից բարձր պատվերների համար",
  "Redeem 1000 pts for ֏5,000 discount": "Փոխարկել 1000 միավորը ֏5,000 զեղչի",
  "Gold unlocks priority production": "Gold կարգը բացում է առաջնահերթ արտադրություն",
  "Points History": "Միավորների պատմություն",
  "Username": "Օգտանուն",
  "Password": "Գաղտնաբառ",
  "Demo credentials hint and failure alert live here in the final auth flow.": "Վերջնական մուտքի հոսքում այստեղ են երևում դեմո տվյալների հուշումը և սխալի ահազանգը։",
  "Baguette": "Բագետ",
  "Croissant": "Կրուասան",
  "Sourdough": "Թթխմորային",
  "Sourdough Loaf": "Թթխմորային հաց",
  "Cake": "Տորթ",
  "Roll": "Ռոլ",
  "Donut": "Դոնաթ",
  "Tart": "Տարտ",
  "Eclair": "Էկլեր",
  "Matnakash": "Մատնաքաշ",
  "Gata": "Գաթա",
  "Flour": "Ալյուր",
  "Butter": "Կարագ",
  "Sugar": "Շաքար",
  "Sesame": "Քունջութ",
  "Water": "Ջուր",
  "Yeast": "Խմորիչ",
  "Salt": "Աղ",
  "CONFIRMED": "Հաստատված",
  "IN_PRODUCTION": "Արտադրության մեջ",
  "OUT_FOR_DELIVERY": "Առաքման մեջ",
  "DELIVERED": "Առաքված",
  "PUBLISHED": "Հրապարակված",
  "APPROVED": "Հաստատված",
  "GENERATED": "Ստեղծված",
  "ACTIVE": "Ակտիվ",
  "PENDING": "Սպասման մեջ",
  "NOT_STARTED": "Չսկսված",
  "IN_PROGRESS": "Ընթացքի մեջ",
  "COMPLETED": "Ավարտված",
  "DRAFT": "Սևագիր",
  "OVERDUE": "Ժամկետանց",
  "READY": "Պատրաստ"
};

const hyTextEntries = Object.entries(hyTextMap).sort((a, b) => b[0].length - a[0].length);

function translateText(text, hy) {
  if (!hy || typeof text !== "string") return text;
  let out = text;
  for (const [en, hyText] of hyTextEntries) {
    out = out.split(en).join(hyText);
  }
  return out;
}

function localizeValue(value, hy, propKey = "") {
  const blocked = new Set(["className", "status", "value", "variant", "size", "type", "style", "onClick", "icon", "key", "id"]);
  if (!hy) return value;
  if (typeof value === "string") return blocked.has(propKey) ? value : translateText(value, hy);
  if (Array.isArray(value)) return value.map((item) => localizeValue(item, hy, propKey));
  if (React.isValidElement(value)) return localizeNode(value, hy);
  if (value && typeof value === "object") {
    if (propKey === "style") return value;
    const next = {};
    for (const [k, v] of Object.entries(value)) next[k] = localizeValue(v, hy, k);
    return next;
  }
  return value;
}

function localizeNode(node, hy) {
  if (!hy) return node;
  if (typeof node === "string") return translateText(node, hy);
  if (Array.isArray(node)) return node.map((child) => localizeNode(child, hy));
  if (!React.isValidElement(node)) return node;
  const nextProps = {};
  for (const [key, value] of Object.entries(node.props || {})) {
    nextProps[key] = key === "children"
      ? React.Children.map(value, (child) => localizeNode(child, hy))
      : localizeValue(value, hy, key);
  }
  return React.cloneElement(node, nextProps);
}

function cn(...parts) {
  return parts.filter(Boolean).join(" ");
}

function Badge({ children, status }) {
  return (
    <span
      className={cn(
        "inline-flex rounded-full px-2 py-0.5 text-xs font-medium",
        statusStyles[status] || "bg-gray-100 text-gray-700"
      )}
    >
      {children || status}
    </span>
  );
}

function Button({ children, variant = "primary", size = "md", className = "", ...props }) {
  const variants = {
    primary: "bg-blue-600 text-white hover:bg-blue-700",
    secondary: "bg-gray-100 text-gray-700 hover:bg-gray-200",
    danger: "bg-red-600 text-white hover:bg-red-700",
    success: "bg-green-600 text-white hover:bg-green-700",
    ghost: "bg-transparent text-gray-700 hover:bg-gray-100",
  };
  const sizes = {
    xs: "px-2.5 py-1.5 text-xs",
    md: "px-4 py-2 text-sm",
    lg: "px-5 py-2.5 text-sm",
  };
  return (
    <button
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-md font-medium transition disabled:cursor-not-allowed disabled:opacity-50",
        variants[variant],
        sizes[size],
        className
      )}
      {...props}
    >
      {children}
    </button>
  );
}

function Card({ title, action, children, className = "" }) {
  return (
    <div className={cn("rounded-2xl border border-gray-200 bg-white p-4 shadow-sm", className)}>
      {(title || action) && (
        <div className="mb-4 flex items-center justify-between gap-3">
          <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
          {action}
        </div>
      )}
      {children}
    </div>
  );
}

function StatCard({ icon: Icon, label, value, hint }) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</p>
          <p className="mt-2 text-2xl font-bold text-gray-900">{value}</p>
          <p className="mt-1 text-xs text-gray-500">{hint}</p>
        </div>
        <div className="rounded-xl bg-blue-50 p-2 text-blue-600">
          <Icon className="h-5 w-5" />
        </div>
      </div>
    </div>
  );
}

function Input({ label, hint, placeholder, value, onChange, rightIcon: Icon }) {
  return (
    <label className="block">
      {label && <div className="mb-1 text-sm font-medium text-gray-700">{label}</div>}
      <div className="relative">
        <input
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none ring-0 transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500"
          placeholder={placeholder}
          value={value}
          onChange={onChange}
        />
        {Icon ? <Icon className="pointer-events-none absolute right-3 top-2.5 h-4 w-4 text-gray-400" /> : null}
      </div>
      {hint && <div className="mt-1 text-xs text-gray-500">{hint}</div>}
    </label>
  );
}

function SectionTitle({ eyebrow, title, subtitle, action }) {
  return (
    <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
      <div>
        {eyebrow ? <div className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-600">{eyebrow}</div> : null}
        <h1 className="mt-1 text-2xl font-bold text-gray-900">{title}</h1>
        {subtitle ? <p className="mt-1 text-sm text-gray-500">{subtitle}</p> : null}
      </div>
      {action}
    </div>
  );
}

function Table({ columns, rows }) {
  return (
    <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              {columns.map((col) => (
                <th
                  key={col}
                  className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
                >
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white">
            {rows.map((row, idx) => (
              <tr key={idx} className="hover:bg-gray-50">
                {row.map((cell, cellIdx) => (
                  <td key={cellIdx} className="whitespace-nowrap px-4 py-3 text-sm text-gray-700">
                    {cell}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Progress({ value }) {
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-gray-100">
      <div className="h-full rounded-full bg-blue-600" style={{ width: `${value}%` }} />
    </div>
  );
}

function SidebarItem({ icon: Icon, label, active, onClick }) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium transition",
        active ? "bg-slate-800 text-white" : "text-slate-300 hover:bg-slate-800 hover:text-white"
      )}
    >
      <Icon className="h-4 w-4" />
      <span>{label}</span>
    </button>
  );
}

function MiniBarChart() {
  const data = [65, 82, 74, 93, 88, 107, 96];
  return (
    <div className="mt-4 flex h-40 items-end gap-2">
      {data.map((v, i) => (
        <div key={i} className="flex-1 rounded-t-xl bg-blue-600/85" style={{ height: `${v}px` }} />
      ))}
    </div>
  );
}

function DashboardScreen({ t }) {
  const departments = [
    { name: "Bread", done: 18, total: 24 },
    { name: "Pastry", done: 9, total: 12 },
    { name: "Lavash", done: 14, total: 16 },
  ];
  return (
    <div className="space-y-6">
      <SectionTitle
        eyebrow="Overview"
        title={t.dashboard}
        subtitle="Real-time operations snapshot for sales, production, inventory, and delivery."
        action={<Button><Plus className="h-4 w-4" /> Quick Action</Button>}
      />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={CircleDollarSign} label={t.revenueToday} value="֏125,000" hint="+12.4% vs yesterday" />
        <StatCard icon={ShoppingCart} label={t.openOrders} value="12" hint="3 require production start" />
        <StatCard icon={Factory} label={t.plansToday} value="3" hint="2 published, 1 in progress" />
        <StatCard icon={Warehouse} label={t.stockValue} value="֏890,000" hint="4 low-stock alerts" />
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.4fr_0.9fr]">
        <Card title="Delivery Timeline" action={<Badge status="ACTIVE">Live</Badge>}>
          <div className="space-y-4">
            {[
              ["Run 01", "08:00 - 09:30", "Delivered", "DELIVERED", 100],
              ["Run 02", "10:00 - 11:30", "Out for delivery", "OUT_FOR_DELIVERY", 65],
              ["Run 03", "13:00 - 14:30", "Ready", "READY", 32],
            ].map(([name, time, desc, status, progress]) => (
              <div key={name}>
                <div className="mb-2 flex items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{name}</div>
                    <div className="text-xs text-gray-500">{time} • {desc}</div>
                  </div>
                  <Badge status={status}>{status}</Badge>
                </div>
                <Progress value={progress} />
              </div>
            ))}
          </div>
        </Card>

        <Card title="Stock Alerts">
          <div className="space-y-3">
            {[
              ["Flour", "Below reorder point", "Critical", "OVERDUE"],
              ["Sugar", "3 days supply remaining", "Warning", "PENDING"],
              ["Butter", "Needs restock", "Warning", "PENDING"],
              ["Sesame", "Used faster than forecast", "Info", "CONFIRMED"],
            ].map(([name, msg, level, status]) => (
              <div key={name} className="flex items-start gap-3 rounded-xl border border-gray-200 p-3">
                <div className="rounded-full bg-red-50 p-2 text-red-600">
                  <AlertTriangle className="h-4 w-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <div className="text-sm font-semibold text-gray-900">{name}</div>
                    <Badge status={status}>{level}</Badge>
                  </div>
                  <div className="mt-1 text-xs text-gray-500">{msg}</div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.1fr_1fr]">
        <Card title="Production Floor">
          <div className="space-y-4">
            {departments.map((d) => {
              const value = Math.round((d.done / d.total) * 100);
              return (
                <div key={d.name} className="rounded-xl border border-gray-200 p-3">
                  <div className="mb-2 flex items-center justify-between gap-2">
                    <div>
                      <div className="text-sm font-semibold text-gray-900">{d.name}</div>
                      <div className="text-xs text-gray-500">{d.done}/{d.total} work orders complete</div>
                    </div>
                    <span className="text-sm font-semibold text-gray-700">{value}%</span>
                  </div>
                  <Progress value={value} />
                </div>
              );
            })}
          </div>
        </Card>

        <Card title="Issues Detector" action={<Button variant="secondary" size="xs">Review All</Button>}>
          <div className="space-y-3">
            {[
              ["Orders without plans", "2 wholesale orders need scheduling"],
              ["Plans without active recipe", "Croissant v3 draft exists but is not active"],
              ["Low stock items", "Flour and butter could block tomorrow morning production"],
            ].map(([title, desc]) => (
              <div key={title} className="rounded-xl bg-gray-50 p-3">
                <div className="text-sm font-semibold text-gray-900">{title}</div>
                <div className="mt-1 text-xs text-gray-500">{desc}</div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card title="Getting Started Wizard">
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          {[
            [true, "Create department"],
            [true, "Add products"],
            [false, "Add recipes"],
            [false, "Configure inventory"],
          ].map(([done, label]) => (
            <div key={label} className="flex items-center gap-3 rounded-xl border border-gray-200 p-3">
              <div className={cn("rounded-full p-1.5", done ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500")}>
                {done ? <Check className="h-4 w-4" /> : <Clock3 className="h-4 w-4" />}
              </div>
              <div className="text-sm font-medium text-gray-700">{label}</div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

function OrdersScreen() {
  const rows = [
    ["#1052", "Armen's Bakery", "+374-94-123456", "10.03.2026", "11.03.2026", <Badge status="CONFIRMED">CONFIRMED</Badge>, "֏30,000", <div className="flex gap-2"><Button size="xs" variant="secondary">Start</Button><Button size="xs" variant="ghost">Cancel</Button></div>],
    ["#1051", "Café Central", "+374-91-553344", "10.03.2026", "10.03.2026", <Badge status="READY">READY</Badge>, "֏18,500", <div className="flex gap-2"><Button size="xs" variant="success">Deliver</Button><Button size="xs" variant="ghost">Edit</Button></div>],
    ["#1050", "Hotel Grand", "+374-77-998877", "09.03.2026", "10.03.2026", <Badge status="IN_PRODUCTION">IN_PRODUCTION</Badge>, "֏67,000", <div className="flex gap-2"><Button size="xs" variant="secondary">Ready</Button><Button size="xs" variant="ghost">Notes</Button></div>],
  ];
  return (
    <div className="space-y-6">
      <SectionTitle
        eyebrow="Sales"
        title="Orders"
        subtitle="Manage the full order lifecycle from draft to delivery."
        action={<Button><Plus className="h-4 w-4" /> New Order</Button>}
      />
      <div className="grid gap-3 lg:grid-cols-[1fr_180px_180px_220px]">
        <Input label="Customer Search" placeholder="Search by name or phone" rightIcon={Search} />
        <Input label="Status" placeholder="CONFIRMED" />
        <Input label="Date Range" placeholder="10.03 - 12.03" />
        <div className="flex items-end gap-2">
          <Button variant="secondary" className="w-full"><Filter className="h-4 w-4" /> Filters</Button>
        </div>
      </div>
      <Table
        columns={["Order #", "Customer", "Phone", "Order Date", "Delivery Date", "Status", "Total", "Actions"]}
        rows={rows}
      />
      <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
        <Card title="Create Order Modal Preview">
          <div className="grid gap-4 md:grid-cols-2">
            <Input label="Customer Name" placeholder="Armen's Bakery" />
            <Input label="Phone" placeholder="+374-94-123456" />
            <Input label="Delivery Date" placeholder="11.03.2026" />
            <div>
              <div className="mb-1 text-sm font-medium text-gray-700">Rush Delivery</div>
              <div className="flex items-center justify-between rounded-md border border-gray-300 px-3 py-2">
                <span className="text-sm text-gray-700">Apply premium</span>
                <div className="h-6 w-11 rounded-full bg-blue-600 p-1">
                  <div className="ml-auto h-4 w-4 rounded-full bg-white" />
                </div>
              </div>
            </div>
          </div>
          <div className="mt-4 space-y-3 rounded-xl border border-gray-200 p-3">
            {[
              ["Baguette", 50, "֏400"],
              ["Croissant", 20, "֏500"],
            ].map(([item, qty, price]) => (
              <div key={item} className="grid gap-3 md:grid-cols-[1fr_110px_110px_40px]">
                <Input label="Product" placeholder={item} />
                <Input label="Qty" placeholder={String(qty)} />
                <Input label="Unit Price" placeholder={price} />
                <div className="flex items-end"><Button variant="danger" size="xs" className="w-full"><Trash2 className="h-4 w-4" /></Button></div>
              </div>
            ))}
          </div>
          <div className="mt-4 flex items-center justify-between border-t border-gray-200 pt-4">
            <Button variant="secondary"><Plus className="h-4 w-4" /> Add Line</Button>
            <div className="text-right">
              <div className="text-xs text-gray-500">Order Total</div>
              <div className="text-xl font-bold text-gray-900">֏30,000</div>
            </div>
          </div>
        </Card>
        <Card title="Status Flow">
          <div className="flex flex-wrap items-center gap-3">
            {[
              "DRAFT",
              "CONFIRMED",
              "IN_PRODUCTION",
              "READY",
              "OUT_FOR_DELIVERY",
              "DELIVERED",
            ].map((s, i, arr) => (
              <React.Fragment key={s}>
                <Badge status={s}>{s}</Badge>
                {i < arr.length - 1 ? <ChevronRight className="h-4 w-4 text-gray-400" /> : null}
              </React.Fragment>
            ))}
          </div>
          <div className="mt-6 space-y-3">
            <div className="rounded-xl bg-blue-50 p-3 text-sm text-blue-800">Confirm and production start actions are prominent for managers and production users.</div>
            <div className="rounded-xl bg-amber-50 p-3 text-sm text-amber-800">Rush orders surface clearly in filters and in row-level actions.</div>
            <div className="rounded-xl bg-red-50 p-3 text-sm text-red-800">Cancel requires a reason modal to preserve auditability.</div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function PosScreen() {
  const products = [
    ["Baguette", "֏400"],
    ["Cake", "֏1,200"],
    ["Roll", "֏300"],
    ["Croissant", "֏500"],
    ["Donut", "֏350"],
    ["Tart", "֏800"],
  ];
  const cart = [
    ["Baguette", 2, "֏800"],
    ["Croissant", 1, "֏500"],
    ["Danish", 2, "֏900"],
  ];
  return (
    <div className="space-y-6">
      <SectionTitle
        eyebrow="Retail"
        title="Point of Sale"
        subtitle="Fast-touch cash desk flow with product grid, cart, and payment states."
        action={<Button variant="secondary"><Building2 className="h-4 w-4" /> Bakery Front</Button>}
      />
      <div className="grid gap-4 xl:grid-cols-[1.45fr_0.95fr]">
        <Card title="Product Grid">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {products.map(([name, price]) => (
              <button key={name} className="rounded-2xl border border-gray-200 bg-white p-4 text-left transition hover:border-blue-300 hover:bg-blue-50">
                <div className="mb-6 flex h-16 items-center justify-center rounded-xl bg-gray-100 text-gray-400">
                  <Package className="h-6 w-6" />
                </div>
                <div className="text-sm font-semibold text-gray-900">{name}</div>
                <div className="mt-1 text-sm text-gray-500">{price}</div>
              </button>
            ))}
          </div>
        </Card>
        <Card title="Cart">
          <div className="space-y-3">
            {cart.map(([name, qty, amount]) => (
              <div key={name} className="flex items-center justify-between gap-3 rounded-xl border border-gray-200 p-3">
                <div>
                  <div className="text-sm font-semibold text-gray-900">{name}</div>
                  <div className="mt-1 flex items-center gap-2 text-xs text-gray-500">
                    <button className="rounded bg-gray-100 px-2 py-0.5">−</button>
                    ×{qty}
                    <button className="rounded bg-gray-100 px-2 py-0.5">+</button>
                  </div>
                </div>
                <div className="text-sm font-semibold text-gray-700">{amount}</div>
              </div>
            ))}
          </div>
          <div className="mt-6 space-y-2 border-t border-gray-200 pt-4 text-sm">
            <div className="flex items-center justify-between"><span className="text-gray-500">Subtotal</span><span className="font-medium">֏2,100</span></div>
            <div className="flex items-center justify-between"><span className="text-gray-500">VAT</span><span className="font-medium">֏420</span></div>
            <div className="flex items-center justify-between text-base font-bold text-gray-900"><span>Total</span><span>֏2,520</span></div>
          </div>
          <div className="mt-4 grid gap-2 sm:grid-cols-2">
            <Button className="w-full"><CircleDollarSign className="h-4 w-4" /> Cash</Button>
            <Button variant="secondary" className="w-full"><CreditCard className="h-4 w-4" /> Card</Button>
          </div>
        </Card>
      </div>
      <div className="grid gap-4 xl:grid-cols-2">
        <Card title="Payment Modal Preview">
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Amount Tendered" placeholder="֏3,000" />
            <Input label="Change" placeholder="֏480" />
          </div>
          <div className="mt-4 rounded-xl border border-dashed border-gray-300 p-4 text-sm text-gray-500">
            Receipt preview, transaction number, business name, itemized totals, and print state live here.
          </div>
        </Card>
        <Card title="End of Day Reconciliation">
          <div className="space-y-3">
            {[
              ["Cash expected", "֏147,500"],
              ["Cash counted", "֏146,800"],
              ["Card subtotal", "֏312,400"],
              ["Overage / shortage", "-֏700"],
            ].map(([label, value]) => (
              <div key={label} className="flex items-center justify-between rounded-xl bg-gray-50 px-3 py-2">
                <span className="text-sm text-gray-600">{label}</span>
                <span className="text-sm font-semibold text-gray-900">{value}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function ProductionPlansScreen() {
  return (
    <div className="space-y-6">
      <SectionTitle
        eyebrow="Production"
        title="Production Plans"
        subtitle="Plan shifts, generate work orders, and visualize production timing."
        action={<Button><Plus className="h-4 w-4" /> New Plan</Button>}
      />
      <Table
        columns={["Plan", "Date", "Shift", "Department", "Status", "Work Orders", "Actions"]}
        rows={[
          ["Morning Batch — March 10", "10.03.2026", "Morning", "Bread", <Badge status="PUBLISHED">PUBLISHED</Badge>, "12", <Button size="xs" variant="secondary">Start</Button>],
          ["Pastry Noon Run", "10.03.2026", "Afternoon", "Pastry", <Badge status="APPROVED">APPROVED</Badge>, "8", <Button size="xs" variant="primary">Publish</Button>],
          ["Lavash Night", "10.03.2026", "Night", "Lavash", <Badge status="GENERATED">GENERATED</Badge>, "5", <Button size="xs" variant="secondary">Approve</Button>],
        ]}
      />
      <div className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
        <Card title="Plan Detail">
          <div className="space-y-3">
            {[
              ["Baguette ×50", "08:00", "IN_PROGRESS"],
              ["Croissant ×30", "08:45", "NOT_STARTED"],
              ["Matnakash ×40", "09:30", "NOT_STARTED"],
            ].map(([name, time, status]) => (
              <div key={name} className="flex items-center justify-between rounded-xl border border-gray-200 p-3">
                <div>
                  <div className="text-sm font-semibold text-gray-900">{name}</div>
                  <div className="text-xs text-gray-500">Scheduled {time}</div>
                </div>
                <Badge status={status}>{status}</Badge>
              </div>
            ))}
          </div>
          <div className="mt-4 rounded-xl bg-gray-50 p-3">
            <div className="text-sm font-semibold text-gray-900">Material Requirements</div>
            <div className="mt-2 grid gap-2 text-sm text-gray-600 sm:grid-cols-2">
              <div>Flour • 42.5 kg</div>
              <div>Butter • 8.2 kg</div>
              <div>Water • 28.0 L</div>
              <div>Yeast • 1.8 kg</div>
            </div>
          </div>
        </Card>
        <Card title="Gantt Schedule View">
          <div className="space-y-4">
            {[
              ["Baguette", 18, 68, "bg-blue-600"],
              ["Croissant", 35, 54, "bg-amber-500"],
              ["Matnakash", 58, 33, "bg-emerald-600"],
            ].map(([name, start, width, cls]) => (
              <div key={name}>
                <div className="mb-2 flex items-center justify-between text-sm">
                  <span className="font-semibold text-gray-900">{name}</span>
                  <span className="text-gray-500">08:00 - 11:00</span>
                </div>
                <div className="relative h-10 rounded-xl bg-gray-100">
                  <div className={cn("absolute top-1 h-8 rounded-xl", cls)} style={{ left: `${start}%`, width: `${width}%` }} />
                </div>
              </div>
            ))}
            <div className="grid grid-cols-6 text-xs text-gray-400">
              {['06:00','07:00','08:00','09:00','10:00','11:00'].map((h) => <div key={h}>{h}</div>)}
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function FloorViewScreen() {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="Production" title="Floor View" subtitle="Tablet-friendly daily execution for workers and shift leads." />
      <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-sm text-gray-500">Plan</div>
            <div className="text-xl font-bold text-gray-900">Morning Batch — March 10</div>
          </div>
          <Badge status="PUBLISHED">PUBLISHED</Badge>
        </div>
      </div>
      <div className="grid gap-4 xl:grid-cols-[1fr_0.95fr]">
        <div className="grid gap-4 md:grid-cols-2">
          {[
            ["Baguette ×50", "IN_PROGRESS", true],
            ["Croissant ×30", "NOT_STARTED", false],
            ["Lavash ×80", "NOT_STARTED", false],
            ["Gata ×20", "COMPLETED", false],
          ].map(([name, status, selected]) => (
            <div key={name} className={cn("rounded-2xl border p-5 shadow-sm", selected ? "border-blue-300 bg-blue-50" : "border-gray-200 bg-white")}>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="text-lg font-semibold text-gray-900">{name}</div>
                  <div className="mt-1"><Badge status={status}>{status}</Badge></div>
                </div>
                <HardHat className="h-5 w-5 text-gray-400" />
              </div>
              <div className="mt-6 flex gap-2">
                {status !== "COMPLETED" ? <Button size="xs">Start</Button> : null}
                <Button size="xs" variant="secondary">Complete</Button>
              </div>
            </div>
          ))}
        </div>
        <Card title="Selected Work Order">
          <div className="mb-4 flex gap-2">
            <Button size="xs">Technology Steps</Button>
            <Button size="xs" variant="secondary">Recipe</Button>
          </div>
          <div className="space-y-3">
            {[
              [true, "Mix dough", "15 min"],
              [true, "First proof", "40 min"],
              [false, "Shape loaves", "20 min"],
              [false, "Second proof", "30 min"],
              [false, "Bake at 220°C", "25 min"],
            ].map(([done, name, time]) => (
              <div key={name} className="flex items-center justify-between rounded-xl border border-gray-200 p-3">
                <div className="flex items-center gap-3">
                  <div className={cn("flex h-5 w-5 items-center justify-center rounded-md border", done ? "border-green-600 bg-green-600 text-white" : "border-gray-300 bg-white text-transparent")}>
                    <Check className="h-3 w-3" />
                  </div>
                  <div>
                    <div className="text-sm font-medium text-gray-900">{name}</div>
                    <div className="text-xs text-gray-500">{time}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-4 rounded-xl bg-gray-50 p-3">
            <div className="text-sm font-semibold text-gray-900">Recipe Snapshot</div>
            <div className="mt-2 grid gap-2 text-sm text-gray-600 sm:grid-cols-2">
              <div>Flour — 12.5 kg</div>
              <div>Water — 8.0 L</div>
              <div>Yeast — 0.25 kg</div>
              <div>Salt — 0.2 kg</div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function InventoryScreen() {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="Inventory" title="Inventory" subtitle="FIFO lots, stock alerts, receiving, transfers, and adjustments." />
      <div className="flex flex-wrap gap-2">
        <Button size="xs">Stock Levels</Button>
        <Button size="xs" variant="secondary">Items</Button>
      </div>
      <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <Table
          columns={["Item", "Location", "Current Qty", "UoM", "Lot #", "Reorder", "Status"]}
          rows={[
            ["Flour", "Main WH", "420", "kg", "FL-2026-03-08", "500", <Badge status="OVERDUE">LOW</Badge>],
            ["Butter", "Cold Room", "52", "kg", "BT-2026-03-06", "60", <Badge status="PENDING">LOW</Badge>],
            ["Sesame", "Main WH", "14", "kg", "SE-2026-03-01", "10", <Badge status="ACTIVE">OK</Badge>],
          ]}
        />
        <Card title="Alerts & Actions">
          <div className="space-y-3">
            {[
              ["Receive Stock", "Receive item, lot, supplier, currency, unit cost"],
              ["Transfer", "Move inventory between locations"],
              ["Adjust", "Waste, spoilage, count correction"],
            ].map(([title, desc]) => (
              <div key={title} className="rounded-xl border border-gray-200 p-3">
                <div className="text-sm font-semibold text-gray-900">{title}</div>
                <div className="mt-1 text-xs text-gray-500">{desc}</div>
              </div>
            ))}
          </div>
          <div className="mt-4 rounded-2xl bg-red-50 p-4 text-sm text-red-800">
            Flour is below reorder point and could block tomorrow morning production.
          </div>
        </Card>
      </div>
      <div className="grid gap-4 xl:grid-cols-3">
        {[
          ["Receive Stock Modal", ["Item", "Quantity", "Unit Cost", "Currency / FX", "Lot #", "Supplier"]],
          ["Transfer Modal", ["Item", "From Location", "To Location", "Qty"]],
          ["Adjust Modal", ["Item", "Qty (+/-)", "Reason", "Notes"]],
        ].map(([title, fields]) => (
          <Card key={title} title={title}>
            <div className="space-y-3">
              {fields.map((field) => <Input key={field} label={field} placeholder={field} />)}
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}

function AiWhatsappScreen({ t }) {
  const conversations = [
    ["Armen's Bakery", "I need 50 baguettes for tomorrow", "10:32 AM", "ACTIVE", false],
    ["Café Central", "Can I change tomorrow's order?", "09:15 AM", "PENDING", false],
    ["Hotel Grand", "Custom order change, need help", "08:45 AM", "OVERDUE", true],
  ];
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="AI Tools" title="AI WhatsApp Conversations" subtitle="Monitor AI order intake, escalations, and draft-order conversion." />
      <div className="flex gap-2">
        <Button size="xs">{t.allConversations}</Button>
        <Button size="xs" variant="secondary">{t.escalated}</Button>
      </div>
      <div className="grid gap-4 xl:grid-cols-[0.85fr_1.35fr]">
        <Card title="Conversation List">
          <div className="space-y-3">
            {conversations.map(([name, snippet, time, status, escalated], idx) => (
              <div key={name} className={cn("rounded-2xl border p-3 text-left", idx === 0 ? "border-blue-300 bg-blue-50" : "border-gray-200 bg-white")}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{name}</div>
                    <div className="mt-1 text-xs text-gray-500">{snippet}</div>
                  </div>
                  <div className="text-right">
                    <div className="text-xs text-gray-400">{time}</div>
                    <div className="mt-1"><Badge status={status}>{escalated ? "ESCALATED" : status}</Badge></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
        <Card title="Chat Detail" action={<Badge status="ACTIVE">Active</Badge>}>
          <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
            <div className="rounded-2xl border border-gray-200 bg-gray-50 p-4">
              <div className="space-y-3">
                {[
                  ["bot", "Hello! I can help you place an order."],
                  ["user", "50 baguettes and 20 croissants for tomorrow morning"],
                  ["bot", "I've noted your order and prepared a draft for confirmation."],
                ].map(([sender, text], idx) => (
                  <div key={idx} className={cn("flex", sender === "bot" ? "justify-start" : "justify-end")}>
                    <div className={cn("max-w-[85%] rounded-2xl px-4 py-3 text-sm shadow-sm", sender === "bot" ? "bg-white text-gray-800" : "bg-blue-600 text-white")}>
                      {text}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="space-y-4">
              <div className="rounded-2xl border border-gray-200 p-4">
                <div className="text-sm font-semibold text-gray-900">Draft Order</div>
                <div className="mt-3 space-y-2 text-sm text-gray-600">
                  <div className="flex items-center justify-between"><span>Baguette</span><span>×50</span></div>
                  <div className="flex items-center justify-between"><span>Croissant</span><span>×20</span></div>
                  <div className="flex items-center justify-between"><span>Delivery</span><span>Mar 11 • Morning</span></div>
                </div>
                <div className="mt-4 grid gap-2 sm:grid-cols-2">
                  <Button>Confirm</Button>
                  <Button variant="secondary">Edit</Button>
                </div>
              </div>
              <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
                Human escalation is visible, with explicit action to resolve and convert the conversation into an operational order.
              </div>
              <Button variant="secondary" className="w-full"><Shield className="h-4 w-4" /> Resolve Escalation</Button>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function AiSuggestionsScreen() {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="AI Tools" title="AI Suggestions" subtitle="Replenishment, demand forecasting, and production recommendations." />
      <div className="grid gap-4 xl:grid-cols-3">
        <Card title="Replenishment">
          <div className="space-y-3">
            {[
              ["Flour", "420 kg", "+300 kg", "91%"],
              ["Butter", "52 kg", "+40 kg", "84%"],
              ["Chocolate", "18 kg", "+12 kg", "72%"],
            ].map(([item, stock, rec, conf]) => (
              <div key={item} className="rounded-xl border border-gray-200 p-3">
                <div className="flex items-center justify-between gap-2">
                  <div className="text-sm font-semibold text-gray-900">{item}</div>
                  <div className="text-xs text-gray-500">Confidence {conf}</div>
                </div>
                <div className="mt-1 text-xs text-gray-500">Current stock: {stock} • Recommended: {rec}</div>
                <div className="mt-3 flex gap-2">
                  <Button size="xs">Create PO</Button>
                  <Button size="xs" variant="secondary">Dismiss</Button>
                </div>
              </div>
            ))}
          </div>
        </Card>
        <Card title="Demand Forecast">
          <div className="text-sm text-gray-500">Historical vs predicted demand</div>
          <MiniBarChart />
        </Card>
        <Card title="Production Suggestions">
          <div className="space-y-3">
            {[
              ["Baguette", "+40 units"],
              ["Croissant", "+25 units"],
              ["Matnakash", "+18 units"],
            ].map(([product, qty]) => (
              <div key={product} className="flex items-center justify-between rounded-xl bg-gray-50 p-3">
                <div>
                  <div className="text-sm font-semibold text-gray-900">{product}</div>
                  <div className="text-xs text-gray-500">Open orders + forecast - current stock</div>
                </div>
                <div className="text-sm font-semibold text-blue-700">{qty}</div>
              </div>
            ))}
          </div>
          <Button className="mt-4 w-full">Create Plan</Button>
        </Card>
      </div>
    </div>
  );
}

function AiPricingScreen() {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="AI Tools" title="AI Pricing & Anomalies" subtitle="Price optimization and anomaly surfacing for management and finance." />
      <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <Card title="Pricing Suggestions">
          <Table
            columns={["Product", "Current", "Suggested", "Change", "Reason", "Confidence"]}
            rows={[
              ["Croissant", "֏500", "֏540", "+8%", "Rising butter cost + strong demand", "89%"],
              ["Baguette", "֏400", "֏390", "-2.5%", "High price sensitivity in retail", "63%"],
              ["Eclair", "֏700", "֏760", "+8.6%", "Premium category momentum", "78%"],
            ]}
          />
        </Card>
        <Card title="Anomalies">
          <div className="space-y-3">
            {[
              ["Critical", "Cost spike", "Butter purchase cost is 16% above expected range"],
              ["Warning", "Revenue dip", "Pastry category revenue is 12% below weekday norm"],
              ["Info", "Margin erosion", "Wholesale discount mix is compressing croissant margin"],
            ].map(([severity, title, desc]) => (
              <div key={title} className="rounded-xl border border-gray-200 p-3">
                <div className="flex items-center justify-between gap-2">
                  <div className="text-sm font-semibold text-gray-900">{title}</div>
                  <Badge status={severity === "Critical" ? "OVERDUE" : severity === "Warning" ? "PENDING" : "CONFIRMED"}>{severity}</Badge>
                </div>
                <div className="mt-1 text-xs text-gray-500">{desc}</div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function PortalCatalogScreen({ t }) {
  const products = [
    ["Baguette", "֏400/pc"],
    ["Sourdough", "֏600/pc"],
    ["Croissant", "֏500/pc"],
    ["Danish", "֏450/pc"],
    ["Lavash", "֏200/pc"],
    ["Eclair", "֏700/pc"],
    ["Matnakash", "֏350/pc"],
    ["Gata", "֏800/pc"],
  ];
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="Customer Portal" title={t.ourProducts} subtitle="Separate customer-facing app with catalog, ordering, loyalty, and account settings." />
      <div className="grid gap-3 lg:grid-cols-[1fr_180px_180px]">
        <Input label="Search" placeholder="bread..." rightIcon={Search} />
        <Input label="Category" placeholder="All" />
        <Input label="Sort" placeholder="Popular" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {products.map(([name, price]) => (
          <Card key={name} className="overflow-hidden p-0">
            <div className="flex h-40 items-center justify-center bg-gray-100 text-gray-400">
              <Package className="h-10 w-10" />
            </div>
            <div className="p-4">
              <div className="text-lg font-semibold text-gray-900">{name}</div>
              <div className="mt-1 text-sm text-gray-500">{price}</div>
              <Button className="mt-4 w-full"><ShoppingCart className="h-4 w-4" /> Add to cart</Button>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}

function CheckoutScreen({ t }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="Customer Portal" title="Shopping Cart & Checkout" subtitle="Customer-specific pricing, rush toggle, delivery date, and loyalty redemption." />
      <div className="grid gap-4 xl:grid-cols-[1.25fr_0.75fr]">
        <Card title="Your Cart">
          <div className="space-y-3">
            {[
              ["Baguette", 50, "֏20,000"],
              ["Croissant", 30, "֏15,000"],
              ["Sourdough Loaf", 20, "֏12,000"],
            ].map(([item, qty, total]) => (
              <div key={item} className="flex items-center justify-between gap-3 rounded-xl border border-gray-200 p-3">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gray-100 text-gray-400">
                    <Package className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{item}</div>
                    <div className="mt-1 flex items-center gap-2 text-xs text-gray-500">
                      <button className="rounded bg-gray-100 px-2 py-0.5">−</button>
                      ×{qty}
                      <button className="rounded bg-gray-100 px-2 py-0.5">+</button>
                      <button className="rounded bg-gray-100 px-2 py-0.5">🗑</button>
                    </div>
                  </div>
                </div>
                <div className="text-sm font-semibold text-gray-700">{total}</div>
              </div>
            ))}
          </div>
        </Card>
        <Card title="Summary">
          <div className="space-y-3">
            <Input label="Delivery Date" placeholder="11.03.2026" />
            <Input label="Special Notes" placeholder="Cut croissants in half" />
            <div>
              <div className="mb-1 text-sm font-medium text-gray-700">Rush Delivery</div>
              <div className="rounded-xl bg-amber-50 p-3 text-sm text-amber-800">⚡ +15% rush premium</div>
            </div>
            <Input label="Delivery Address" placeholder="Yerevan, Arabkir" />
          </div>
          <div className="mt-6 space-y-2 border-t border-gray-200 pt-4 text-sm">
            <div className="flex items-center justify-between"><span className="text-gray-500">Subtotal</span><span>֏47,000</span></div>
            <div className="flex items-center justify-between"><span className="text-gray-500">Discount</span><span>-֏2,350</span></div>
            <div className="flex items-center justify-between"><span className="text-gray-500">Rush</span><span>+֏6,698</span></div>
            <div className="flex items-center justify-between text-base font-bold text-gray-900"><span>Total</span><span>֏51,348</span></div>
          </div>
          <Button className="mt-4 w-full"><Send className="h-4 w-4" /> {t.placeOrder}</Button>
        </Card>
      </div>
    </div>
  );
}

function LoyaltyScreen() {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="Customer Portal" title="My Loyalty" subtitle="Tier status, points history, and redemption patterns." />
      <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
        <Card title="Tier Status">
          <div className="rounded-2xl bg-gradient-to-r from-slate-900 to-slate-700 p-6 text-white">
            <div className="text-sm uppercase tracking-wide text-slate-300">Silver Member</div>
            <div className="mt-2 text-3xl font-bold">2,450 / 5,000</div>
            <div className="mt-1 text-sm text-slate-200">Points toward Gold</div>
            <div className="mt-4 h-3 overflow-hidden rounded-full bg-white/15">
              <div className="h-full w-[49%] rounded-full bg-white" />
            </div>
            <div className="mt-3 text-sm text-slate-200">Discount 5% • Free delivery on orders over ֏50K</div>
          </div>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl bg-gray-50 p-3 text-sm text-gray-700">Redeem 1000 pts for ֏5,000 discount</div>
            <div className="rounded-xl bg-gray-50 p-3 text-sm text-gray-700">Gold unlocks priority production</div>
          </div>
        </Card>
        <Card title="Points History">
          <div className="space-y-3">
            {[
              ["+250 pts", "Order #1042", "Mar 8, 2026"],
              ["-500 pts", "Redeemed", "Mar 5, 2026"],
              ["+180 pts", "Order #1038", "Mar 3, 2026"],
              ["+320 pts", "Order #1035", "Feb 28, 2026"],
            ].map(([points, ref, date]) => (
              <div key={points + ref} className="flex items-center justify-between rounded-xl border border-gray-200 p-3">
                <div>
                  <div className="text-sm font-semibold text-gray-900">{points}</div>
                  <div className="text-xs text-gray-500">{ref}</div>
                </div>
                <div className="text-xs text-gray-400">{date}</div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function pick(hy, en, hyText) {
  return hy ? hyText : en;
}

function SelectField({ label, value, onChange, options }) {
  return (
    <label className="block">
      {label ? <div className="mb-1 text-sm font-medium text-gray-700">{label}</div> : null}
      <select
        value={value}
        onChange={onChange}
        className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500"
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function MiniLineChart() {
  const pts = [60, 72, 68, 88, 92, 85, 110, 102];
  return (
    <div className="mt-4 flex h-40 items-end gap-2">
      {pts.map((v, i) => (
        <div key={i} className="flex-1 rounded-t-xl bg-blue-200">
          <div className="w-full rounded-t-xl bg-blue-600" style={{ height: `${v}px` }} />
        </div>
      ))}
    </div>
  );
}

function DonutSummary() {
  return (
    <div className="flex items-center gap-5">
      <div className="relative h-28 w-28 rounded-full bg-gray-100">
        <div
          className="absolute inset-0 rounded-full"
          style={{
            background:
              "conic-gradient(#2563EB 0 48%, #F59E0B 48% 74%, #16A34A 74% 90%, #E5E7EB 90% 100%)",
          }}
        />
        <div className="absolute inset-4 rounded-full bg-white" />
      </div>
      <div className="space-y-2 text-sm">
        <div className="flex items-center gap-2 text-gray-600"><span className="h-2.5 w-2.5 rounded-full bg-blue-600" /> Confirmed</div>
        <div className="flex items-center gap-2 text-gray-600"><span className="h-2.5 w-2.5 rounded-full bg-amber-500" /> In production</div>
        <div className="flex items-center gap-2 text-gray-600"><span className="h-2.5 w-2.5 rounded-full bg-green-600" /> Delivered</div>
        <div className="flex items-center gap-2 text-gray-600"><span className="h-2.5 w-2.5 rounded-full bg-gray-300" /> Other</div>
      </div>
    </div>
  );
}

function MobileFrame({ title, lines }) {
  return (
    <div className="mx-auto w-[250px] rounded-[28px] border-8 border-slate-900 bg-white p-3 shadow-xl">
      <div className="mx-auto mb-3 h-1.5 w-20 rounded-full bg-slate-800" />
      <div className="rounded-[20px] bg-gray-50 p-4">
        <div className="text-sm font-semibold text-gray-900">{title}</div>
        <div className="mt-4 space-y-2">
          {lines.map((line) => (
            <div key={line} className="rounded-xl bg-white px-3 py-2 text-xs text-gray-600 shadow-sm">
              {line}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function RecipesScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle
        eyebrow={pick(hy, "Production", "Արտադրություն")}
        title={pick(hy, "Recipes", "Բաղադրատոմսեր")}
        subtitle={pick(hy, "Recipe versioning, ingredients, and technology steps.", "Բաղադրատոմսերի տարբերակներ, բաղադրիչներ և տեխնոլոգիական քայլեր։")}
        action={<Button><Plus className="h-4 w-4" /> {pick(hy, "New Recipe", "Նոր բաղադրատոմս")}</Button>}
      />
      <div className="grid gap-3 lg:grid-cols-[220px_1fr_180px]">
        <SelectField label={pick(hy, "Department", "Բաժին")} value="bread" onChange={() => {}} options={[{ value: "bread", label: pick(hy, "Bread", "Հաց") }, { value: "pastry", label: pick(hy, "Pastry", "Խմորեղեն") }]} />
        <Input label={pick(hy, "Product Search", "Ապրանքի որոնում")} placeholder={pick(hy, "Search recipe...", "Որոնել բաղադրատոմս...")} rightIcon={Search} />
        <div className="flex items-end"><Button variant="secondary" className="w-full"><Filter className="h-4 w-4" /> {pick(hy, "Filters", "Ֆիլտրեր")}</Button></div>
      </div>
      <Table
        columns={hy ? ["Ապրանք", "Տարբերակ", "Կարգավիճակ", "Ստեղծվել է", "Գործողություն"] : ["Product", "Version", "Status", "Created", "Actions"]}
        rows={[
          ["Baguette", "v3", <Badge status="ACTIVE">ACTIVE</Badge>, "07.03.2026", <Button size="xs">{pick(hy, "Activate", "Ակտիվացնել")}</Button>],
          ["Croissant", "v2", <Badge status="DRAFT">DRAFT</Badge>, "05.03.2026", <Button size="xs" variant="secondary">{pick(hy, "Edit", "Խմբագրել")}</Button>],
        ]}
      />
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Card title={pick(hy, "Ingredients Tab", "Բաղադրիչներ")}> 
          <Table
            columns={hy ? ["Ապրանք", "Քանակ", "Չափ․ միավոր", "Կորուստ %", "Ռեժիմ"] : ["Item", "Qty", "UoM", "Waste %", "Mode"]}
            rows={[
              ["Flour", "12.5", "kg", "1.0", "AUTO"],
              ["Water", "8.0", "L", "0.0", "AUTO"],
              ["Yeast", "0.25", "kg", "0.5", "MANUAL"],
            ]}
          />
          <div className="mt-4"><Button variant="secondary"><Plus className="h-4 w-4" /> {pick(hy, "Add Ingredient", "Ավելացնել բաղադրիչ")}</Button></div>
        </Card>
        <Card title={pick(hy, "Technology Steps", "Տեխնոլոգիական քայլեր")}>
          <div className="space-y-3">
            {[
              ["1", pick(hy, "Mix dough", "Խառնել խմորը"), pick(hy, "15 min", "15 րոպե")],
              ["2", pick(hy, "First proof", "Առաջին խմորում"), pick(hy, "40 min", "40 րոպե")],
              ["3", pick(hy, "Bake at 220°C", "Թխել 220°C-ում"), pick(hy, "25 min", "25 րոպե")],
            ].map(([step, name, time]) => (
              <div key={step} className="flex items-center justify-between rounded-xl border border-gray-200 p-3">
                <div>
                  <div className="text-sm font-semibold text-gray-900">{step}. {name}</div>
                  <div className="text-xs text-gray-500">{time}</div>
                </div>
                <Button size="xs" variant="secondary">{pick(hy, "Edit", "Խմբագրել")}</Button>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function ProductsScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle
        eyebrow={pick(hy, "Catalog", "Կատալոգ")}
        title={pick(hy, "Products", "Ապրանքներ")}
        subtitle={pick(hy, "Manage products, sale units, VAT, and prices.", "Կառավարեք ապրանքները, վաճառքի միավորները, ԱԱՀ-ն և գները։")}
        action={<Button><Plus className="h-4 w-4" /> {pick(hy, "New Product", "Նոր ապրանք")}</Button>}
      />
      <div className="grid gap-3 lg:grid-cols-[1fr_220px_180px]">
        <Input label={pick(hy, "Search", "Որոնում")} placeholder={pick(hy, "Search products...", "Որոնել ապրանք...")} rightIcon={Search} />
        <SelectField label={pick(hy, "Department", "Բաժին")} value="all" onChange={() => {}} options={[{ value: "all", label: pick(hy, "All", "Բոլորը") }, { value: "bread", label: pick(hy, "Bread", "Հաց") }, { value: "pastry", label: pick(hy, "Pastry", "Խմորեղեն") }]} />
        <div className="flex items-end"><Button variant="secondary" className="w-full"><Filter className="h-4 w-4" /> {pick(hy, "Filter", "Ֆիլտր")}</Button></div>
      </div>
      <Table
        columns={hy ? ["Անուն", "Բաժին", "Վաճառքի միավոր", "Չափ․ միավոր", "Գին", "ԱԱՀ %", "Կարգավիճակ", "Գործողություն"] : ["Name", "Department", "Sale Unit", "UoM", "Price", "VAT %", "Status", "Actions"]}
        rows={[
          ["Baguette", pick(hy, "Bread", "Հաց"), "pc", "pc", "֏400", "20", <Badge status="ACTIVE">ACTIVE</Badge>, <div className="flex gap-2"><Button size="xs" variant="secondary">Edit</Button><Button size="xs" variant="ghost">Archive</Button></div>],
          ["Croissant", pick(hy, "Pastry", "Խմորեղեն"), "pc", "pc", "֏500", "20", <Badge status="ACTIVE">ACTIVE</Badge>, <div className="flex gap-2"><Button size="xs" variant="secondary">Edit</Button><Button size="xs" variant="ghost">Archive</Button></div>],
        ]}
      />
      <Card title={pick(hy, "Create / Edit Product Modal", "Ապրանքի ստեղծում / խմբագրում")}>
        <div className="grid gap-4 md:grid-cols-2">
          <Input label={pick(hy, "Name", "Անուն")} placeholder="Croissant" />
          <SelectField label={pick(hy, "Department", "Բաժին")} value="pastry" onChange={() => {}} options={[{ value: "pastry", label: pick(hy, "Pastry", "Խմորեղեն") }]} />
          <Input label={pick(hy, "Sale Unit", "Վաճառքի միավոր")} placeholder="pc" />
          <Input label={pick(hy, "Base UoM", "Հիմնական չափ․ միավոր")} placeholder="pc" />
          <Input label={pick(hy, "Price", "Գին")} placeholder="֏500" />
          <Input label={pick(hy, "VAT Rate", "ԱԱՀ տոկոսադրույք")} placeholder="20" />
        </div>
      </Card>
    </div>
  );
}

function DepartmentsScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Operations", "Օպերացիաներ")} title={pick(hy, "Departments", "Բաժիններ")} subtitle={pick(hy, "Lead times, warehouse mode, and status.", "Առաջատար ժամանակ, պահեստի ռեժիմ և կարգավիճակ։")} action={<Button><Plus className="h-4 w-4" /> {pick(hy, "New Department", "Նոր բաժին")}</Button>} />
      <Table
        columns={hy ? ["Անուն", "Lead Time", "Warehouse Mode", "Կարգավիճակ", "Գործողություն"] : ["Name", "Lead Time", "Warehouse Mode", "Status", "Actions"]}
        rows={[
          [pick(hy, "Bread", "Հաց"), pick(hy, "6 hrs", "6 ժամ"), "SHARED", <Badge status="ACTIVE">ACTIVE</Badge>, <Button size="xs" variant="secondary">Edit</Button>],
          [pick(hy, "Pastry", "Խմորեղեն"), pick(hy, "8 hrs", "8 ժամ"), "ISOLATED", <Badge status="ACTIVE">ACTIVE</Badge>, <Button size="xs" variant="secondary">Edit</Button>],
        ]}
      />
      <Card title={pick(hy, "Department Modal", "Բաժնի պատուհան")}>
        <div className="grid gap-4 md:grid-cols-3">
          <Input label={pick(hy, "Department Name", "Բաժնի անվանում")} placeholder={pick(hy, "Bread", "Հաց")} />
          <Input label={pick(hy, "Lead Time (hours)", "Lead Time (ժամ)")} placeholder="6" />
          <SelectField label={pick(hy, "Warehouse Mode", "Պահեստի ռեժիմ")} value="shared" onChange={() => {}} options={[{ value: "shared", label: "SHARED" }, { value: "isolated", label: "ISOLATED" }]} />
        </div>
      </Card>
    </div>
  );
}

function ReportsScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Insights", "Վերլուծություն")}
        title={pick(hy, "Reports", "Հաշվետվություններ")}
        subtitle={pick(hy, "Management reporting widgets from the design brief.", "Կառավարման հաշվետվությունների վիջեթներ ըստ դիզայնի բրիֆի։")}
      />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={CircleDollarSign} label={pick(hy, "Today Revenue", "Այսօրվա եկամուտ")} value="֏125,000" hint={pick(hy, "+12% vs yesterday", "+12% երեկվա համեմատ")} />
        <StatCard icon={ShoppingCart} label={pick(hy, "Week Revenue", "Շաբաթվա եկամուտ")} value="֏742,000" hint={pick(hy, "Wholesale leading", "Մեծածախը առաջատար է")} />
        <StatCard icon={Factory} label={pick(hy, "Plans Completed", "Ավարտված պլաններ")} value="18" hint={pick(hy, "Avg lead time 6.4h", "Միջին lead time 6.4 ժ")} />
        <StatCard icon={Warehouse} label={pick(hy, "Stock Alerts", "Պահեստային ահազանգեր")} value="4" hint={pick(hy, "2 critical", "2 կրիտիկական")} />
      </div>
      <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
        <Card title={pick(hy, "Top Products", "Լավագույն ապրանքներ")}>
          <MiniBarChart />
        </Card>
        <Card title={pick(hy, "Orders Summary", "Պատվերների ամփոփում")}>
          <DonutSummary />
        </Card>
      </div>
      <Card title={pick(hy, "Production Summary", "Արտադրության ամփոփում")}>
        <Table
          columns={hy ? ["Բաժին", "Պլաններ", "WO", "Միջին lead time", "Լրացում"] : ["Department", "Plans", "WOs Finished", "Avg Lead Time", "Completion"]}
          rows={[
            [pick(hy, "Bread", "Հաց"), "8", "42", "5.8h", <div className="w-28"><Progress value={88} /></div>],
            [pick(hy, "Pastry", "Խմորեղեն"), "6", "29", "7.1h", <div className="w-28"><Progress value={74} /></div>],
          ]}
        />
      </Card>
    </div>
  );
}

function TechnologistScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Production QA", "Արտադրական QA")} title={pick(hy, "Technologist View", "Տեխնոլոգի տեսք")} subtitle={pick(hy, "Recipe health, draft versions, and production readiness.", "Բաղադրատոմսերի առողջություն, draft տարբերակներ և արտադրական պատրաստվածություն։")} />
      <div className="grid gap-4 lg:grid-cols-3">
        {[
          [pick(hy, "Active recipe coverage", "Ակտիվ բաղադրատոմսերի ծածկույթ"), "92%", "ACTIVE"],
          [pick(hy, "Needs review", "Պահանջում է վերանայում"), "3", "PENDING"],
          [pick(hy, "Blocked plans", "Արգելափակված պլաններ"), "1", "OVERDUE"],
        ].map(([title, value, status]) => (
          <Card key={title} title={title} action={<Badge status={status}>{status}</Badge>}>
            <div className="text-3xl font-bold text-gray-900">{value}</div>
          </Card>
        ))}
      </div>
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Table
          columns={hy ? ["Ապրանք", "Տարբերակ", "Առողջություն", "Խնդիր"] : ["Product", "Version", "Health", "Issue"]}
          rows={[
            ["Croissant", "v2", <Badge status="PENDING">REVIEW</Badge>, pick(hy, "Butter ratio changed", "Փոխվել է կարագի հարաբերակցությունը")],
            ["Baguette", "v3", <Badge status="ACTIVE">READY</Badge>, pick(hy, "No issues", "Խնդիր չկա")],
            ["Eclair", "v1", <Badge status="OVERDUE">BLOCKED</Badge>, pick(hy, "Missing active packaging item", "Բացակայում է ակտիվ փաթեթավորման item")],
          ]}
        />
        <Card title={pick(hy, "Selected Recipe Health Detail", "Ընտրված բաղադրատոմսի մանրամասներ")}>
          <div className="space-y-3">
            <div className="rounded-xl bg-red-50 p-3 text-sm text-red-800">{pick(hy, "Packaging ingredient is not active. Production plan publication should be blocked.", "Փաթեթավորման բաղադրիչը ակտիվ չէ։ Արտադրական պլանի հրապարակումը պետք է արգելափակվի։")}</div>
            <div className="rounded-xl bg-gray-50 p-3 text-sm text-gray-700">{pick(hy, "Last updated by Technologist on 08.03.2026.", "Վերջին թարմացումը կատարել է տեխնոլոգը 08.03.2026-ին։")}</div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function AdminScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "System", "Համակարգ")} title={pick(hy, "Admin", "Ադմին")}
        subtitle={pick(hy, "Users, roles, and system configuration.", "Օգտագործողներ, դերեր և համակարգի կարգավորումներ։")}
      />
      <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
        <Table
          columns={hy ? ["Օգտագործող", "Դեր", "Բաժին", "Կարգավիճակ", "Գործողություն"] : ["User", "Role", "Department", "Status", "Actions"]}
          rows={[
            ["Anna", "Admin", "HQ", <Badge status="ACTIVE">ACTIVE</Badge>, <Button size="xs" variant="secondary">Edit</Button>],
            ["Gor", "Manager", pick(hy, "Bread", "Հաց"), <Badge status="ACTIVE">ACTIVE</Badge>, <Button size="xs" variant="secondary">Edit</Button>],
            ["Mariam", "Finance", "HQ", <Badge status="PENDING">INVITED</Badge>, <Button size="xs">Resend</Button>],
          ]}
        />
        <Card title={pick(hy, "System Config", "Համակարգի կարգավորումներ")}>
          <div className="space-y-3">
            <SelectField label={pick(hy, "Default Currency", "Լռելյայն արժույթ")} value="amd" onChange={() => {}} options={[{ value: "amd", label: "AMD" }, { value: "usd", label: "USD" }]} />
            <SelectField label={pick(hy, "Date Format", "Ամսաթվի ձևաչափ")} value="ddmmyyyy" onChange={() => {}} options={[{ value: "ddmmyyyy", label: "DD.MM.YYYY" }]} />
            <div className="rounded-xl bg-blue-50 p-3 text-sm text-blue-800">{pick(hy, "Role-based filtering should hide inaccessible modules instead of graying them out.", "Դերային ֆիլտրացիան պետք է թաքցնի անհասանելի մոդուլները, ոչ թե մոխրացնի։")}</div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function SuppliersScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Procurement", "Գնումներ")} title={pick(hy, "Suppliers", "Մատակարարներ")} subtitle={pick(hy, "Suppliers, purchase orders, and API configuration.", "Մատակարարներ, գնման պատվերներ և API կարգավորում։")} action={<Button><Plus className="h-4 w-4" /> {pick(hy, "New Supplier", "Նոր մատակարար")}</Button>} />
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Table
          columns={hy ? ["Մատակարար", "Կոնտակտ", "Արժույթ", "Կարգավիճակ"] : ["Supplier", "Contact", "Currency", "Status"]}
          rows={[
            ["FlourCo", "+374-91-445566", "AMD", <Badge status="ACTIVE">ACTIVE</Badge>],
            ["ButterTrade", "+374-99-112233", "EUR", <Badge status="ACTIVE">ACTIVE</Badge>],
          ]}
        />
        <Card title={pick(hy, "Purchase Orders", "Գնման պատվերներ")}>
          <div className="space-y-3">
            {[
              ["PO-0041", "FlourCo", "֏220,000", "CONFIRMED"],
              ["PO-0042", "ButterTrade", "€780", "PENDING"],
            ].map(([po, supplier, amount, status]) => (
              <div key={po} className="flex items-center justify-between rounded-xl border border-gray-200 p-3">
                <div>
                  <div className="text-sm font-semibold text-gray-900">{po}</div>
                  <div className="text-xs text-gray-500">{supplier} • {amount}</div>
                </div>
                <Badge status={status}>{status}</Badge>
              </div>
            ))}
          </div>
          <div className="mt-4 rounded-xl bg-gray-50 p-3 text-sm text-gray-600">{pick(hy, "API Config tab is reserved for central bank / supplier integrations.", "API Config բաժինը նախատեսված է կենտրոնական բանկի / մատակարարների ինտեգրացիաների համար։")}</div>
        </Card>
      </div>
    </div>
  );
}

function DeliveriesScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Logistics", "Լոգիստիկա")} title={pick(hy, "Deliveries", "Առաքումներ")} subtitle={pick(hy, "Runs, manifests, and per-order actions.", "Առաքման ռաններ, manifest-ներ և պատվերի գործողություններ։")} action={<Button><Plus className="h-4 w-4" /> {pick(hy, "New Delivery Run", "Նոր առաքման ռան")}</Button>} />
      <Table
        columns={hy ? ["Run #", "Վարորդ", "Մեքենա", "Ամսաթիվ", "Պատվերներ", "Կարգավիճակ", "Գործողություն"] : ["Run #", "Driver", "Vehicle", "Date", "# Orders", "Status", "Actions"]}
        rows={[
          ["RUN-11", "Hovhannes", "35 OL 501", "10.03.2026", "8", <Badge status="OUT_FOR_DELIVERY">LIVE</Badge>, <Button size="xs" variant="secondary">Open</Button>],
          ["RUN-12", "Aram", "34 QQ 778", "10.03.2026", "5", <Badge status="READY">READY</Badge>, <Button size="xs">Start</Button>],
        ]}
      />
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Card title={pick(hy, "Manifest Preview", "Manifest նախադիտում")}>
          <div className="space-y-3">
            {[
              ["#1052", "Armen's Bakery", "Arabkir, Yerevan", "READY"],
              ["#1053", "Café Central", "Kentron, Yerevan", "PENDING"],
            ].map(([order, customer, address, status]) => (
              <div key={order} className="rounded-xl border border-gray-200 p-3">
                <div className="flex items-center justify-between gap-2">
                  <div className="text-sm font-semibold text-gray-900">{order} • {customer}</div>
                  <Badge status={status}>{status}</Badge>
                </div>
                <div className="mt-1 text-xs text-gray-500">{address}</div>
              </div>
            ))}
          </div>
        </Card>
        <Card title={pick(hy, "Per-order Actions", "Պատվերի գործողություններ")}>
          <div className="flex flex-wrap gap-2">
            <Button size="xs" variant="success">{pick(hy, "Complete", "Ավարտել")}</Button>
            <Button size="xs" variant="danger">{pick(hy, "Fail", "Չստացվեց")}</Button>
            <Button size="xs" variant="secondary">{pick(hy, "Redeliver", "Վերաառաքել")}</Button>
            <Button size="xs" variant="ghost">{pick(hy, "Waive Charge", "Չեղարկել վճարը")}</Button>
          </div>
        </Card>
      </div>
    </div>
  );
}

function InvoicesScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Finance", "Ֆինանսներ")} title={pick(hy, "Invoices", "Հաշիվներ")} subtitle={pick(hy, "B2B invoicing, payments, discounts, and credit checks.", "B2B հաշիվներ, վճարումներ, զեղչեր և վարկային ստուգումներ։")} />
      <div className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
        <Table
          columns={hy ? ["Invoice #", "Հաճախորդ", "Issue", "Due", "Գումար", "Կարգավիճակ", "Գործողություն"] : ["Invoice #", "Customer", "Issue Date", "Due Date", "Amount", "Status", "Actions"]}
          rows={[
            ["INV-203", "Hotel Grand", "01.03.2026", "15.03.2026", "֏420,000", <Badge status="PENDING">ISSUED</Badge>, <Button size="xs" variant="secondary">View</Button>],
            ["INV-204", "Café Central", "03.03.2026", "10.03.2026", "֏97,500", <Badge status="OVERDUE">OVERDUE</Badge>, <Button size="xs">Record Payment</Button>],
          ]}
        />
        <Card title={pick(hy, "Discount Rules & Credit", "Զեղչերի կանոններ և կրեդիտ")}>
          <div className="space-y-3 text-sm text-gray-700">
            <div className="rounded-xl border border-gray-200 p-3">Hotel Grand • Wholesale • 8% category discount</div>
            <div className="rounded-xl border border-gray-200 p-3">Café Central • Credit limit ֏250,000 • Current balance ֏97,500</div>
            <div className="rounded-xl bg-red-50 p-3 text-red-800">{pick(hy, "Credit check failed for one customer above limit.", "Մեկ հաճախորդի մոտ կրեդիտային սահմանաչափը գերազանցված է։")}</div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function CustomersAdminScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "CRM", "CRM")} title={pick(hy, "Customers", "Հաճախորդներ")} subtitle={pick(hy, "Admin-side customer management, catalog preview, and portal orders.", "Ադմին կողմից հաճախորդների կառավարում, կատալոգի նախադիտում և պորտալի պատվերներ։")} action={<Button><Plus className="h-4 w-4" /> {pick(hy, "Register Customer", "Գրանցել հաճախորդ")}</Button>} />
      <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
        <Table
          columns={hy ? ["Անուն", "Կոնտակտ", "Հեռախոս", "Էլ․ փոստ", "Տիպ", "Կարգավիճակ"] : ["Name", "Contact", "Phone", "Email", "Type", "Status"]}
          rows={[
            ["Armen's Bakery", "Armen", "+374-94-123456", "armen@bakery.am", "Wholesale", <Badge status="ACTIVE">ACTIVE</Badge>],
            ["Café Central", "Nane", "+374-91-223344", "nane@cafe.am", "HoReCa", <Badge status="ACTIVE">ACTIVE</Badge>],
          ]}
        />
        <Card title={pick(hy, "Portal Catalog Preview", "Պորտալի կատալոգի նախադիտում")}>
          <div className="grid gap-3 sm:grid-cols-2">
            {[
              ["Baguette", "֏400"],
              ["Croissant", "֏500"],
              ["Gata", "֏800"],
              ["Lavash", "֏200"],
            ].map(([name, price]) => (
              <div key={name} className="rounded-xl border border-gray-200 p-3">
                <div className="text-sm font-semibold text-gray-900">{name}</div>
                <div className="text-xs text-gray-500">{price}</div>
              </div>
            ))}
          </div>
        </Card>
      </div>
      <Card title={pick(hy, "Portal Orders", "Պորտալի պատվերներ")}>
        <Table
          columns={hy ? ["Order #", "Հաճախորդ", "Ամսաթիվ", "Կարգավիճակ", "Ընդամենը"] : ["Order #", "Customer", "Date", "Status", "Total"]}
          rows={[
            ["#1101", "Armen's Bakery", "09.03.2026", <Badge status="CONFIRMED">CONFIRMED</Badge>, "֏51,348"],
            ["#1102", "Café Central", "09.03.2026", <Badge status="DELIVERED">DELIVERED</Badge>, "֏22,600"],
          ]}
        />
      </Card>
    </div>
  );
}

function BackofficeLoyaltyScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Retention", "Պահպանում")} title={pick(hy, "Loyalty Program", "Հավատարմության ծրագիր")} subtitle={pick(hy, "Tiers, balances, and transaction history.", "Տարբեր աստիճաններ, մնացորդներ և պատմություն։")} />
      <div className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
        <Card title={pick(hy, "Tiers", "Աստիճաններ")}>
          <Table
            columns={hy ? ["Անուն", "Min Points", "Զեղչ %", "Բոնուսներ"] : ["Name", "Min Points", "Discount %", "Perks"]}
            rows={[
              ["Bronze", "0", "2", pick(hy, "Base access", "Հիմնական մուտք")],
              ["Silver", "2,500", "5", pick(hy, "Free delivery over threshold", "Անվճար առաքում շեմից վեր")],
              ["Gold", "5,000", "8", pick(hy, "Priority production", "Առաջնահերթ արտադրություն")],
            ]}
          />
        </Card>
        <Card title={pick(hy, "Balances & History", "Մնացորդներ և պատմություն")}>
          <div className="rounded-2xl bg-gradient-to-r from-slate-900 to-slate-700 p-5 text-white">
            <div className="text-sm text-slate-300">Armen's Bakery</div>
            <div className="mt-2 text-3xl font-bold">2,450</div>
            <div className="mt-1 text-sm text-slate-200">{pick(hy, "points • Silver tier", "միավոր • Silver կարգ")}</div>
            <div className="mt-4 h-3 overflow-hidden rounded-full bg-white/15"><div className="h-full w-[49%] rounded-full bg-white" /></div>
          </div>
          <div className="mt-4 space-y-2">
            {[
              ["AWARD", "+250", "Order #1042"],
              ["REDEEM", "-500", "March promo"],
            ].map(([type, pts, ref]) => (
              <div key={type + pts} className="flex items-center justify-between rounded-xl border border-gray-200 p-3 text-sm">
                <span>{type}</span><span>{pts}</span><span className="text-gray-500">{ref}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function ReportBuilderScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Analytics", "Վերլուծություն")} title={pick(hy, "Report Builder", "Հաշվետվությունների կառուցող")}
        subtitle={pick(hy, "Compose reports from KPI blocks and export them.", "Կազմեք հաշվետվություններ KPI բլոկներից և արտահանեք դրանք։")}
        action={<Button><Plus className="h-4 w-4" /> {pick(hy, "Create Report", "Ստեղծել հաշվետվություն")}</Button>}
      />
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Card title={pick(hy, "Custom Reports", "Անհատական հաշվետվություններ")}>
          <Table
            columns={hy ? ["Անուն", "Ստեղծվել է", "# Բլոկ", "Գործողություն"] : ["Name", "Created", "# Blocks", "Actions"]}
            rows={[
              [pick(hy, "Daily Management Pack", "Օրական կառավարման փաթեթ"), "08.03.2026", "6", <Button size="xs">Run</Button>],
              [pick(hy, "Inventory Health", "Պահեստի առողջություն"), "07.03.2026", "4", <Button size="xs" variant="secondary">Edit</Button>],
            ]}
          />
        </Card>
        <Card title={pick(hy, "KPI Catalog", "KPI կատալոգ")}>
          <div className="grid gap-3 sm:grid-cols-2">
            {[
              pick(hy, "Revenue", "Եկամուտ"),
              pick(hy, "Production", "Արտադրություն"),
              pick(hy, "Inventory", "Պահեստ"),
              pick(hy, "Orders", "Պատվերներ"),
            ].map((item) => (
              <div key={item} className="rounded-xl border border-gray-200 p-3">
                <div className="text-sm font-semibold text-gray-900">{item}</div>
                <div className="mt-1 text-xs text-gray-500">{pick(hy, "Preview sample output", "Նախադիտել օրինակային արդյունքը")}</div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function SubscriptionsScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Platform", "Պլատֆորմ")}
        title={pick(hy, "Subscriptions", "Բաժանորդագրություններ")}
        subtitle={pick(hy, "SaaS tier cards, limits, and tenant assignment.", "SaaS աստիճաններ, սահմանաչափեր և tenant-ների բաշխում։")}
      />
      <div className="grid gap-4 lg:grid-cols-4">
        {[
          ["Free", "֏0", ["2 users", "1 department"]],
          ["Starter", "֏19,000", ["5 users", "AI off"]],
          ["Professional", "֏49,000", ["15 users", "AI on"]],
          ["Enterprise", "Custom", ["Unlimited", "API access"]],
        ].map(([name, price, features]) => (
          <Card key={name} title={name} action={<Badge status="ACTIVE">{pick(hy, "Tier", "Աստիճան")}</Badge>}>
            <div className="text-2xl font-bold text-gray-900">{price}</div>
            <div className="mt-3 space-y-2 text-sm text-gray-600">
              {features.map((feature) => <div key={feature}>• {feature}</div>)}
            </div>
          </Card>
        ))}
      </div>
      <Card title={pick(hy, "Tenant Assignment", "Tenant-ների բաշխում")}>
        <Table
          columns={hy ? ["Tenant ID", "Ընթացիկ աստիճան", "Տրվել է", "Feature Check"] : ["Tenant ID", "Current Tier", "Assigned Date", "Feature Check"]}
          rows={[
            ["tenant-001", "Professional", "01.03.2026", "AI Pricing ✓"],
            ["tenant-002", "Starter", "28.02.2026", "API Access ✕"],
          ]}
        />
      </Card>
    </div>
  );
}

function ExchangeRatesScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Finance Tools", "Ֆինանսական գործիքներ")} title={pick(hy, "Exchange Rates", "Փոխարժեքներ")}
        subtitle={pick(hy, "Historical lookup, converter, and external fetch.", "Պատմական որոնում, փոխարկիչ և արտաքին բեռնում։")}
        action={<Button><ArrowLeftRight className="h-4 w-4" /> {pick(hy, "Fetch External", "Բեռնել արտաքին")}</Button>}
      />
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Card title={pick(hy, "Add Rate", "Ավելացնել փոխարժեք")}>
          <div className="grid gap-4 md:grid-cols-2">
            <SelectField label={pick(hy, "Base Currency", "Հիմնական արժույթ")} value="USD" onChange={() => {}} options={[{ value: "USD", label: "USD" }, { value: "EUR", label: "EUR" }]} />
            <SelectField label={pick(hy, "Target Currency", "Թիրախ արժույթ")} value="AMD" onChange={() => {}} options={[{ value: "AMD", label: "AMD" }]} />
            <Input label={pick(hy, "Rate", "Փոխարժեք")} placeholder="385.40" />
            <Input label={pick(hy, "Effective Date", "Գործողության ամսաթիվ")} placeholder="10.03.2026" />
          </div>
        </Card>
        <Card title={pick(hy, "Converter", "Փոխարկիչ")}>
          <div className="grid gap-4 md:grid-cols-3">
            <Input label={pick(hy, "Amount", "Գումար")} placeholder="780" />
            <SelectField label={pick(hy, "From", "Որտեղից")} value="EUR" onChange={() => {}} options={[{ value: "EUR", label: "EUR" }]} />
            <SelectField label={pick(hy, "To", "Դեպի")} value="AMD" onChange={() => {}} options={[{ value: "AMD", label: "AMD" }]} />
          </div>
          <div className="mt-4 rounded-xl bg-blue-50 p-4 text-lg font-semibold text-blue-900">€780 = ֏300,612</div>
        </Card>
      </div>
      <Table
        columns={hy ? ["Base", "Target", "Փոխարժեք", "Ամսաթիվ", "Աղբյուր"] : ["Base", "Target", "Rate", "Date", "Source"]}
        rows={[
          ["USD", "AMD", "385.40", "10.03.2026", "API"],
          ["EUR", "AMD", "385.40", "10.03.2026", "Manual"],
        ]}
      />
    </div>
  );
}

function DriverManagementScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Logistics", "Լոգիստիկա")} title={pick(hy, "Driver Management", "Վարորդների կառավարում")}
        subtitle={pick(hy, "Active sessions, packaging checks, and payments.", "Ակտիվ սեսիաներ, փաթեթավորման ստուգումներ և վճարումներ։")}
      />
      <div className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
        <Card title={pick(hy, "Active Sessions", "Ակտիվ սեսիաներ")}>
          <div className="space-y-3">
            {[
              ["Hovhannes", "35 OL 501", "6 stops left"],
              ["Aram", "34 QQ 778", "3 stops left"],
            ].map(([driver, vehicle, note]) => (
              <div key={driver} className="rounded-xl border border-gray-200 p-3">
                <div className="flex items-center justify-between gap-2">
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{driver}</div>
                    <div className="text-xs text-gray-500">{vehicle} • {note}</div>
                  </div>
                  <MapPin className="h-4 w-4 text-gray-400" />
                </div>
              </div>
            ))}
          </div>
        </Card>
        <Card title={pick(hy, "Packaging Check", "Փաթեթավորման ստուգում")}>
          <Table
            columns={hy ? ["Run", "Order", "Expected", "Confirmed", "Discrepancy"] : ["Run", "Order", "Expected", "Confirmed", "Discrepancy"]}
            rows={[
              ["RUN-11", "#1052", "12", "12", <Badge status="ACTIVE">OK</Badge>],
              ["RUN-11", "#1053", "8", "7", <Badge status="OVERDUE">-1</Badge>],
            ]}
          />
        </Card>
      </div>
      <Card title={pick(hy, "Payments", "Վճարումներ")}>
        <Table
          columns={hy ? ["Order", "Session", "Amount", "Method", "Status", "Time"] : ["Order #", "Session", "Amount", "Method", "Status", "Time"]}
          rows={[
            ["#1052", "RUN-11", "֏30,000", "CASH", <Badge status="COMPLETED">PAID</Badge>, "10:34"],
            ["#1053", "RUN-11", "֏18,500", "CARD", <Badge status="PENDING">PENDING</Badge>, "11:05"],
          ]}
        />
      </Card>
    </div>
  );
}

function MobileAdminScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Mobile", "Մոբայլ")} title={pick(hy, "Mobile Admin", "Մոբայլ ադմին")}
        subtitle={pick(hy, "Registered devices and push notification history.", "Գրանցված սարքեր և push ծանուցումների պատմություն։")}
      />
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Table
          columns={hy ? ["Հաճախորդ", "Սարք", "Պլատֆորմ", "Թոքեն", "Գրանցվել է"] : ["Customer", "Device", "Platform", "Token", "Registered"]}
          rows={[
            ["Armen's Bakery", "iPhone 14", "iOS", "apns_••••51", "04.03.2026"],
            ["Café Central", "Galaxy S24", "Android", "fcm_••••92", "03.03.2026"],
          ]}
        />
        <Card title={pick(hy, "Notification History", "Ծանուցումների պատմություն")}>
          <div className="space-y-3">
            {[
              [pick(hy, "Order Status", "Պատվերի կարգավիճակ"), "SENT", "10.03.2026 10:40"],
              [pick(hy, "Promotion", "Ակցիա"), "FAILED", "09.03.2026 18:20"],
            ].map(([title, status, time]) => (
              <div key={title + time} className="rounded-xl border border-gray-200 p-3">
                <div className="flex items-center justify-between gap-2">
                  <div className="text-sm font-semibold text-gray-900">{title}</div>
                  <Badge status={status === "SENT" ? "ACTIVE" : "OVERDUE"}>{status}</Badge>
                </div>
                <div className="mt-1 text-xs text-gray-500">{time}</div>
              </div>
            ))}
          </div>
          <div className="mt-4"><Button className="w-full"><Bell className="h-4 w-4" /> {pick(hy, "Send Notification", "Ուղարկել ծանուցում")}</Button></div>
        </Card>
      </div>
    </div>
  );
}

function CustomerAuthScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Customer Portal", "Հաճախորդի պորտալ")} title={pick(hy, "Customer Login / Register", "Հաճախորդի մուտք / գրանցում")}
        subtitle={pick(hy, "Separate customer authentication flow with Armenian phone format.", "Առանձին հաճախորդի վավերացում հայկական հեռախոսահամարի ձևաչափով։")}
      />
      <div className="grid gap-4 xl:grid-cols-2">
        <Card title={pick(hy, "Sign In", "Մուտք") }>
          <div className="space-y-3">
            <Input label={pick(hy, "Phone Number", "Հեռախոսահամար")} placeholder="+374-XX-XXXXXX" />
            <Input label={pick(hy, "Password", "Գաղտնաբառ")} placeholder="••••••••" />
            <Button className="w-full">{pick(hy, "Sign In", "Մուտք գործել")}</Button>
            <div className="text-xs text-gray-500">{pick(hy, "Forgot Password", "Մոռացե՞լ եք գաղտնաբառը")}</div>
          </div>
        </Card>
        <Card title={pick(hy, "Register", "Գրանցում")}>
          <div className="grid gap-3 md:grid-cols-2">
            <Input label={pick(hy, "Business Name", "Բիզնեսի անվանում")} placeholder="Armen's Bakery" />
            <Input label={pick(hy, "Contact Person", "Կոնտակտ անձ")} placeholder="Armen" />
            <Input label={pick(hy, "Phone Number", "Հեռախոսահամար")} placeholder="+374-XX-XXXXXX" />
            <Input label={pick(hy, "Email", "Էլ․ փոստ")} placeholder="armen@bakery.am" />
            <Input label={pick(hy, "Password", "Գաղտնաբառ")} placeholder="••••••••" />
            <Input label={pick(hy, "Confirm Password", "Կրկնել գաղտնաբառը")} placeholder="••••••••" />
          </div>
          <Button className="mt-4 w-full">{pick(hy, "Create Account", "Ստեղծել հաշիվ")}</Button>
        </Card>
      </div>
    </div>
  );
}

function OrderConfirmationScreen({ hy }) {
  return (
    <div className="space-y-6">
      <Card className="bg-gradient-to-r from-green-600 to-emerald-600 p-8 text-white">
        <div className="flex flex-col items-start gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <div className="text-sm uppercase tracking-[0.25em] text-white/80">{pick(hy, "Success", "Հաջողվեց")}</div>
            <div className="mt-2 text-3xl font-bold">{pick(hy, "Order #1101 confirmed", "Պատվեր #1101-ը հաստատված է")}</div>
            <div className="mt-2 text-sm text-white/90">{pick(hy, "You will receive order updates here and in WhatsApp.", "Կստանաք պատվերի թարմացումները այստեղ և WhatsApp-ում։")}</div>
          </div>
          <Check className="h-10 w-10" />
        </div>
      </Card>
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Card title={pick(hy, "Order Summary", "Պատվերի ամփոփում")}>
          <div className="space-y-2 text-sm text-gray-700">
            <div className="flex items-center justify-between"><span>Baguette × 50</span><span>֏20,000</span></div>
            <div className="flex items-center justify-between"><span>Croissant × 20</span><span>֏10,000</span></div>
            <div className="flex items-center justify-between border-t border-gray-200 pt-2 font-semibold"><span>{pick(hy, "Total", "Ընդամենը")}</span><span>֏30,000</span></div>
          </div>
        </Card>
        <Card title={pick(hy, "Next Actions", "Հաջորդ քայլեր")}>
          <div className="space-y-3">
            <Button className="w-full">{pick(hy, "Track Order", "Հետևել պատվերին")}</Button>
            <Button className="w-full" variant="secondary">{pick(hy, "Back to Catalog", "Վերադառնալ կատալոգ")}</Button>
          </div>
        </Card>
      </div>
    </div>
  );
}

function MyOrdersScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Customer Portal", "Հաճախորդի պորտալ")} title={pick(hy, "My Orders", "Իմ պատվերները")}
        subtitle={pick(hy, "Active orders, history, and simple delivery tracking.", "Ակտիվ պատվերներ, պատմություն և առաքման պարզ հետևում։")}
      />
      <Table
        columns={hy ? ["Order #", "Ամսաթիվ", "Կարգավիճակ", "Ընդամենը", "Գործողություն"] : ["Order #", "Date", "Status", "Total", "Actions"]}
        rows={[
          ["#1101", "09.03.2026", <Badge status="OUT_FOR_DELIVERY">OUT_FOR_DELIVERY</Badge>, "֏51,348", <Button size="xs">Track</Button>],
          ["#1098", "06.03.2026", <Badge status="DELIVERED">DELIVERED</Badge>, "֏22,600", <Button size="xs" variant="secondary">Reorder</Button>],
        ]}
      />
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Card title={pick(hy, "Live Tracking", "Կենդանի հետևում")}>
          <div className="space-y-3">
            {[
              pick(hy, "Order confirmed", "Պատվերը հաստատված է"),
              pick(hy, "Production started", "Արտադրությունը սկսվել է"),
              pick(hy, "Out for delivery", "Առաքման մեջ է"),
            ].map((item, idx) => (
              <div key={item} className="flex items-center gap-3">
                <div className={cn("h-3 w-3 rounded-full", idx < 3 ? "bg-blue-600" : "bg-gray-300")} />
                <div className="text-sm text-gray-700">{item}</div>
              </div>
            ))}
          </div>
        </Card>
        <Card title={pick(hy, "Recent History", "Վերջին պատմություն")}>
          <div className="space-y-3">
            {[
              ["#1097", "Delivered", "֏18,400"],
              ["#1096", "Delivered", "֏42,700"],
            ].map(([order, status, amount]) => (
              <div key={order} className="flex items-center justify-between rounded-xl border border-gray-200 p-3 text-sm">
                <span>{order}</span><span>{status}</span><span>{amount}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function CustomerProfileScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Customer Portal", "Հաճախորդի պորտալ")} title={pick(hy, "Profile & Settings", "Պրոֆիլ և կարգավորումներ")}
        subtitle={pick(hy, "Business profile, addresses, password, and notifications.", "Բիզնես պրոֆիլ, հասցեներ, գաղտնաբառ և ծանուցումներ։")}
      />
      <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Card title={pick(hy, "Business Profile", "Բիզնես պրոֆիլ")}>
          <div className="grid gap-3 md:grid-cols-2">
            <Input label={pick(hy, "Business Name", "Բիզնեսի անվանում")} placeholder="Armen's Bakery" />
            <Input label={pick(hy, "Contact Person", "Կոնտակտ անձ")} placeholder="Armen" />
            <Input label={pick(hy, "Phone", "Հեռախոս")} placeholder="+374-94-123456" />
            <Input label={pick(hy, "Email", "Էլ․ փոստ")} placeholder="armen@bakery.am" />
          </div>
        </Card>
        <Card title={pick(hy, "Addresses & Notifications", "Հասցեներ և ծանուցումներ")}>
          <div className="space-y-3">
            <div className="rounded-xl border border-gray-200 p-3 text-sm">Arabkir, Yerevan</div>
            <div className="rounded-xl border border-gray-200 p-3 text-sm">Kentron, Yerevan</div>
            <div className="rounded-xl bg-gray-50 p-3 text-sm text-gray-600">{pick(hy, "WhatsApp, email, and push preferences live here.", "WhatsApp, email և push նախընտրությունները այստեղ են։")}</div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function WhatsAppFlowsScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "WhatsApp", "WhatsApp")} title={pick(hy, "WhatsApp Flows", "WhatsApp հոսքեր")} subtitle={pick(hy, "New order, status updates, and human escalation flows.", "Նոր պատվեր, կարգավիճակի թարմացումներ և մարդու էսկալացիայի հոսքեր։")} />
      <div className="grid gap-4 xl:grid-cols-3">
        <Card title={pick(hy, "New Order", "Նոր պատվեր")}>
          <div className="space-y-2 text-sm">
            <div className="rounded-2xl bg-white p-3 shadow-sm">Hi, I need 50 baguettes for tomorrow</div>
            <div className="rounded-2xl bg-blue-600 p-3 text-white shadow-sm">✅ Confirm | ✏️ Edit | ❌ Cancel</div>
          </div>
        </Card>
        <Card title={pick(hy, "Status Update", "Կարգավիճակի թարմացում")}>
          <div className="rounded-2xl bg-blue-600 p-3 text-sm text-white shadow-sm">📦 Order #1052 Update: OUT FOR DELIVERY • Driver: Hovhannes • ETA: 10:30</div>
        </Card>
        <Card title={pick(hy, "Escalation", "Էսկալացիա")}>
          <div className="space-y-2 text-sm">
            <div className="rounded-2xl bg-white p-3 shadow-sm">I want to change a custom order</div>
            <div className="rounded-2xl bg-red-50 p-3 text-red-800 shadow-sm">⏳ Team member will respond shortly</div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function NotificationTemplatesScreen({ hy }) {
  const templates = [
    pick(hy, "Order Confirmation", "Պատվերի հաստատում"),
    pick(hy, "Production Started", "Արտադրությունը սկսվել է"),
    pick(hy, "Ready for Delivery", "Պատրաստ է առաքման"),
    pick(hy, "Out for Delivery", "Առաքման մեջ է"),
    pick(hy, "Delivered", "Առաքված է"),
    pick(hy, "Payment Reminder", "Վճարման հիշեցում"),
    pick(hy, "Stock Alert", "Պահեստի ահազանգ"),
    pick(hy, "Promotional", "Գովազդային")
  ];
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Templates", "Կաղապարներ")} title={pick(hy, "Notification Templates", "Ծանուցումների կաղապարներ")} subtitle={pick(hy, "Preview cards for the 8 notification types listed in the spec.", "Նախադիտման քարտեր սպեցիֆիկացիայի 8 ծանուցման տեսակների համար։")} />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {templates.map((title) => (
          <Card key={title} title={title}>
            <div className="rounded-xl bg-gray-50 p-3 text-sm text-gray-600">{pick(hy, "Template preview body with variables and delivery channel metadata.", "Կաղապարի նախադիտման տեքստ փոփոխականներով և առաքման ալիքով։")}</div>
          </Card>
        ))}
      </div>
    </div>
  );
}

function MobileGalleryScreen({ hy }) {
  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={pick(hy, "Mobile Futures", "Մոբայլ ապագա")} title={pick(hy, "Mobile App Screens", "Մոբայլ էկրաններ")} subtitle={pick(hy, "Customer, floor worker, and driver mobile concepts.", "Հաճախորդի, արտադրամասի աշխատողի և վարորդի մոբայլ կոնցեպտներ։")} />
      <div className="grid gap-6 xl:grid-cols-3">
        <MobileFrame title={pick(hy, "Customer App", "Հաճախորդի հավելված")} lines={[pick(hy, "Catalog", "Կատալոգ"), pick(hy, "Cart (3)", "Զամբյուղ (3)"), pick(hy, "Orders", "Պատվերներ")]} />
        <MobileFrame title={pick(hy, "Floor Worker", "Արտադրամասի աշխատող")} lines={[pick(hy, "Shift view", "Հերթափոխ"), pick(hy, "WO cards", "WO քարտեր"), pick(hy, "Checklist", "Ստուգաթերթ")]} />
        <MobileFrame title={pick(hy, "Driver App", "Վարորդի հավելված")} lines={[pick(hy, "Route", "Երթուղի"), pick(hy, "Manifest", "Manifest"), pick(hy, "Payments", "Վճարումներ")]} />
      </div>
    </div>
  );
}

function ErrorStateScreen({ hy }) {
  return (
    <div className="flex min-h-[520px] items-center justify-center">
      <div className="max-w-xl text-center">
        <div className="text-7xl font-black text-slate-900">404</div>
        <div className="mt-3 text-2xl font-bold text-gray-900">{pick(hy, "This tray came out empty", "Այս տաշտակը դատարկ դուրս եկավ")}</div>
        <p className="mt-3 text-sm text-gray-500">{pick(hy, "The design spec includes an error page, so this prototype now has one too.", "Դիզայնի սպեցիֆիկացիայում կա նաև error page, ուստի այս պրոտոտիպում էլ այն ավելացվել է։")}</p>
        <div className="mt-6 flex justify-center gap-2">
          <Button>{pick(hy, "Back to Dashboard", "Վերադառնալ վահանակ")}</Button>
          <Button variant="secondary">{pick(hy, "Open Catalog", "Բացել կատալոգը")}</Button>
        </div>
      </div>
    </div>
  );
}

function LoginShowcase({ t }) {
  return (
    <div className="min-h-[720px] rounded-[28px] bg-gradient-to-br from-slate-900 to-slate-800 p-6 shadow-2xl">
      <div className="mx-auto flex min-h-[650px] max-w-md items-center justify-center">
        <div className="w-full rounded-3xl bg-white p-8 shadow-2xl">
          <div className="mb-8 text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-600 text-white shadow-lg">
              <ChefHat className="h-8 w-8" />
            </div>
            <h1 className="mt-4 text-3xl font-bold text-gray-900">BreadCost</h1>
            <p className="mt-2 text-sm text-gray-500">{t.bakeryManagement}</p>
          </div>
          <div className="space-y-4">
            <Input label="Username" placeholder="manager@breadcost.am" />
            <Input label="Password" placeholder="••••••••" />
            <Button className="w-full" size="lg">{t.login}</Button>
          </div>
          <div className="mt-5 rounded-xl bg-gray-50 p-3 text-xs text-gray-500">
            Demo credentials hint and failure alert live here in the final auth flow.
          </div>
        </div>
      </div>
    </div>
  );
}

export default function BreadCostDesignPrototype() {
  const [language, setLanguage] = useState("en");
  const [appMode, setAppMode] = useState("backoffice");
  const [screen, setScreen] = useState("dashboard");

  const t = translations[language];
  const hy = language === "hy";

  const backOfficeItems = [
    { key: "dashboard", label: t.dashboard, icon: LayoutDashboard },
    { key: "orders", label: t.orders, icon: ShoppingCart },
    { key: "pos", label: t.pos, icon: CreditCard },
    { key: "productionPlans", label: t.productionPlans, icon: Factory },
    { key: "floor", label: t.floor, icon: HardHat },
    { key: "recipes", label: pick(hy, "Recipes", "Բաղադրատոմսեր"), icon: BookOpen },
    { key: "products", label: t.products, icon: Package },
    { key: "departments", label: t.departments, icon: Building2 },
    { key: "inventory", label: t.inventory, icon: Warehouse },
    { key: "reports", label: t.reports, icon: BarChart3 },
    { key: "aiWhatsapp", label: t.aiWhatsapp, icon: MessageCircle },
    { key: "aiSuggestions", label: t.aiSuggestions, icon: Sparkles },
    { key: "aiPricing", label: t.aiPricing, icon: TrendingUp },
  ];

  const portalItems = [
    { key: "catalog", label: "Catalog", icon: Package },
    { key: "checkout", label: "Checkout", icon: ShoppingCart },
    { key: "myOrders", label: pick(hy, "My Orders", "Իմ պատվերները"), icon: ClipboardList },
    { key: "loyalty", label: "Loyalty", icon: Star },
    { key: "profile", label: pick(hy, "Profile", "Պրոֆիլ"), icon: User },
  ];

  const allScreenOptions = [
    { value: "dashboard", label: pick(hy, "Dashboard", "Վահանակ") },
    { value: "orders", label: pick(hy, "Orders", "Պատվերներ") },
    { value: "pos", label: "POS" },
    { value: "productionPlans", label: pick(hy, "Production Plans", "Արտադրական պլաններ") },
    { value: "floor", label: pick(hy, "Floor View", "Արտադրամաս") },
    { value: "recipes", label: pick(hy, "Recipes", "Բաղադրատոմսեր") },
    { value: "products", label: pick(hy, "Products", "Ապրանքներ") },
    { value: "departments", label: pick(hy, "Departments", "Բաժիններ") },
    { value: "inventory", label: pick(hy, "Inventory", "Պահեստ") },
    { value: "reports", label: pick(hy, "Reports", "Հաշվետվություններ") },
    { value: "technologist", label: pick(hy, "Technologist View", "Տեխնոլոգի տեսք") },
    { value: "admin", label: pick(hy, "Admin", "Ադմին") },
    { value: "suppliers", label: pick(hy, "Suppliers", "Մատակարարներ") },
    { value: "deliveries", label: pick(hy, "Deliveries", "Առաքումներ") },
    { value: "invoices", label: pick(hy, "Invoices", "Հաշիվներ") },
    { value: "customersAdmin", label: pick(hy, "Customers", "Հաճախորդներ") },
    { value: "loyaltyAdmin", label: pick(hy, "Loyalty Program", "Հավատարմության ծրագիր") },
    { value: "reportBuilder", label: pick(hy, "Report Builder", "Հաշվետվությունների կառուցող") },
    { value: "subscriptions", label: pick(hy, "Subscriptions", "Բաժանորդագրություններ") },
    { value: "exchangeRates", label: pick(hy, "Exchange Rates", "Փոխարժեքներ") },
    { value: "aiWhatsapp", label: pick(hy, "AI WhatsApp", "AI WhatsApp") },
    { value: "aiSuggestions", label: pick(hy, "AI Suggestions", "AI առաջարկներ") },
    { value: "aiPricing", label: pick(hy, "AI Pricing", "AI գնագոյացում") },
    { value: "driver", label: pick(hy, "Driver Management", "Վարորդների կառավարում") },
    { value: "mobileAdmin", label: pick(hy, "Mobile Admin", "Մոբայլ ադմին") },
    { value: "customerAuth", label: pick(hy, "Customer Login/Register", "Հաճախորդի մուտք/գրանցում") },
    { value: "catalog", label: pick(hy, "Catalog", "Կատալոգ") },
    { value: "checkout", label: pick(hy, "Checkout", "Checkout") },
    { value: "orderConfirmation", label: pick(hy, "Order Confirmation", "Պատվերի հաստատում") },
    { value: "myOrders", label: pick(hy, "My Orders", "Իմ պատվերները") },
    { value: "loyalty", label: pick(hy, "Loyalty Dashboard", "Հավատարմության վահանակ") },
    { value: "profile", label: pick(hy, "Profile & Settings", "Պրոֆիլ և կարգավորումներ") },
    { value: "whatsAppFlows", label: pick(hy, "WhatsApp Flows", "WhatsApp հոսքեր") },
    { value: "notifications", label: pick(hy, "Notification Templates", "Ծանուցումների կաղապարներ") },
    { value: "mobileGallery", label: pick(hy, "Mobile Screens", "Մոբայլ էկրաններ") },
    { value: "notFound", label: pick(hy, "Error / 404", "Սխալ / 404") },
    { value: "login", label: pick(hy, "Staff Login", "Աշխատակիցների մուտք") },
  ];

  const content = useMemo(() => {
    if (screen === "login") return LoginShowcase({ t });
    if (screen === "dashboard") return DashboardScreen({ t });
    if (screen === "orders") return OrdersScreen();
    if (screen === "pos") return PosScreen();
    if (screen === "productionPlans") return ProductionPlansScreen();
    if (screen === "floor") return FloorViewScreen();
    if (screen === "recipes") return RecipesScreen({ hy });
    if (screen === "products") return ProductsScreen({ hy });
    if (screen === "departments") return DepartmentsScreen({ hy });
    if (screen === "inventory") return InventoryScreen();
    if (screen === "reports") return ReportsScreen({ hy });
    if (screen === "technologist") return TechnologistScreen({ hy });
    if (screen === "admin") return AdminScreen({ hy });
    if (screen === "suppliers") return SuppliersScreen({ hy });
    if (screen === "deliveries") return DeliveriesScreen({ hy });
    if (screen === "invoices") return InvoicesScreen({ hy });
    if (screen === "customersAdmin") return CustomersAdminScreen({ hy });
    if (screen === "loyaltyAdmin") return BackofficeLoyaltyScreen({ hy });
    if (screen === "reportBuilder") return ReportBuilderScreen({ hy });
    if (screen === "subscriptions") return SubscriptionsScreen({ hy });
    if (screen === "exchangeRates") return ExchangeRatesScreen({ hy });
    if (screen === "aiWhatsapp") return AiWhatsappScreen({ t });
    if (screen === "aiSuggestions") return AiSuggestionsScreen();
    if (screen === "aiPricing") return AiPricingScreen();
    if (screen === "driver") return DriverManagementScreen({ hy });
    if (screen === "mobileAdmin") return MobileAdminScreen({ hy });
    if (screen === "customerAuth") return CustomerAuthScreen({ hy });
    if (screen === "catalog") return PortalCatalogScreen({ t });
    if (screen === "checkout") return CheckoutScreen({ t });
    if (screen === "orderConfirmation") return OrderConfirmationScreen({ hy });
    if (screen === "myOrders") return MyOrdersScreen({ hy });
    if (screen === "loyalty") return LoyaltyScreen();
    if (screen === "profile") return CustomerProfileScreen({ hy });
    if (screen === "whatsAppFlows") return WhatsAppFlowsScreen({ hy });
    if (screen === "notifications") return NotificationTemplatesScreen({ hy });
    if (screen === "mobileGallery") return MobileGalleryScreen({ hy });
    if (screen === "notFound") return ErrorStateScreen({ hy });
    return DashboardScreen({ t });
  }, [screen, t, hy]);

  const isPortal = appMode === "portal";
  const items = isPortal ? portalItems : backOfficeItems;

  const app = (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <div className="border-b border-gray-200 bg-white">
        <div className="mx-auto flex max-w-[1800px] flex-wrap items-center justify-between gap-3 px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-blue-600 text-white shadow-lg">
              <ChefHat className="h-6 w-6" />
            </div>
            <div>
              <div className="text-lg font-bold">BreadCost</div>
              <div className="text-xs text-gray-500">Design prototype built from your uploaded system spec</div>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Button variant={!isPortal ? "primary" : "secondary"} size="xs" onClick={() => { setAppMode("backoffice"); if (["catalog", "checkout", "loyalty"].includes(screen)) setScreen("dashboard"); }}>
              Back-Office
            </Button>
            <Button variant={isPortal ? "primary" : "secondary"} size="xs" onClick={() => { setAppMode("portal"); if (!["catalog", "checkout", "loyalty"].includes(screen)) setScreen("catalog"); }}>
              {t.customerPortal}
            </Button>
            <Button variant={screen === "login" ? "primary" : "secondary"} size="xs" onClick={() => setScreen("login")}>Login</Button>
            <div className="flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-2 py-1.5">
              <Globe className="h-4 w-4 text-gray-500" />
              <select
                value={language}
                onChange={(e) => setLanguage(e.target.value)}
                className="bg-transparent text-sm font-medium text-gray-700 outline-none"
              >
                <option value="en">English</option>
                <option value="hy">Հայերեն</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {screen === "login" ? (
        <div className="mx-auto max-w-[1800px] p-6">{content}</div>
      ) : (
        <div className="mx-auto grid max-w-[1800px] gap-6 p-6 lg:grid-cols-[260px_minmax(0,1fr)]">
          <aside className="rounded-[28px] bg-slate-900 p-4 text-white shadow-xl">
            <div className="mb-4 rounded-2xl bg-slate-800 p-4">
              <div className="flex items-center gap-3">
                <div className="rounded-xl bg-blue-600 p-2">
                  <ChefHat className="h-5 w-5" />
                </div>
                <div>
                  <div className="font-semibold">BreadCost</div>
                  <div className="text-xs text-slate-400">ERP / design shell</div>
                </div>
              </div>
            </div>

            <div className="space-y-6">
              <div>
                <div className="mb-2 px-3 text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Overview</div>
                <div className="space-y-1">
                  {items.map((item) => (
                    <SidebarItem
                      key={item.key}
                      icon={item.icon}
                      label={item.label}
                      active={screen === item.key}
                      onClick={() => setScreen(item.key)}
                    />
                  ))}
                </div>
              </div>

              {!isPortal && (
                <div>
                  <div className="mb-2 px-3 text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Spec Coverage</div>
                  <div className="space-y-2 rounded-2xl bg-slate-800 p-3 text-sm text-slate-300">
                    <div className="flex items-center justify-between"><span>Foundations</span><Check className="h-4 w-4 text-green-400" /></div>
                    <div className="flex items-center justify-between"><span>App Shell</span><Check className="h-4 w-4 text-green-400" /></div>
                    <div className="flex items-center justify-between"><span>Core Operations</span><Check className="h-4 w-4 text-green-400" /></div>
                    <div className="flex items-center justify-between"><span>AI Screens</span><Check className="h-4 w-4 text-green-400" /></div>
                    <div className="flex items-center justify-between"><span>Portal Preview</span><Check className="h-4 w-4 text-green-400" /></div>
                  </div>
                </div>
              )}
            </div>
          </aside>

          <main className="min-w-0 space-y-6">
            <div className="flex flex-col gap-3 rounded-[28px] border border-gray-200 bg-white px-5 py-4 shadow-sm md:flex-row md:items-center md:justify-between">
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <span>{isPortal ? "Customer Portal" : "Back-Office"}</span>
                <ChevronRight className="h-4 w-4" />
                <span className="font-medium text-gray-900">{items.find((i) => i.key === screen)?.label || (screen === "login" ? "Login" : "Dashboard")}</span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <div className="hidden items-center gap-3 md:flex">
                  <div className="flex items-center gap-2 rounded-xl border border-gray-200 bg-gray-50 px-3 py-2">
                    <Search className="h-4 w-4 text-gray-400" />
                    <select
                      value={screen}
                      onChange={(e) => setScreen(e.target.value)}
                      className="bg-transparent text-sm text-gray-700 outline-none"
                    >
                      {allScreenOptions.map((option) => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                      ))}
                    </select>
                  </div>
                  <div className="flex items-center gap-2 rounded-xl border border-gray-200 bg-gray-50 px-3 py-2">
                    <Globe className="h-4 w-4 text-gray-400" />
                    <select
                      value={language}
                      onChange={(e) => setLanguage(e.target.value)}
                      className="bg-transparent text-sm text-gray-700 outline-none"
                    >
                      <option value="en">English</option>
                      <option value="hy">Հայերեն</option>
                    </select>
                  </div>
                </div>
                <Button variant="secondary" size="xs"><Bell className="h-4 w-4" /> Alerts</Button>
                <Button variant="secondary" size="xs"><User className="h-4 w-4" /> Admin</Button>
                <Button variant="ghost" size="xs"><LogOut className="h-4 w-4" /> Out</Button>
              </div>
            </div>
            {content}
          </main>
        </div>
      )}
    </div>
  );

  return hy ? localizeNode(app, hy) : app;
}
