package com.gezimos.katapult.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Rocket
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

val materialIcons: List<Pair<String, ImageVector>> = listOf(
    "AC Unit" to Icons.Outlined.AcUnit,
    "Account Balance" to Icons.Outlined.AccountBalance,
    "Alarm" to Icons.Outlined.Alarm,
    "Album" to Icons.Outlined.Album,
    "Article" to Icons.AutoMirrored.Outlined.Article,
    "Auto Stories" to Icons.Outlined.AutoStories,
    "Battery" to Icons.Outlined.BatteryFull,
    "Bluetooth" to Icons.Outlined.Bluetooth,
    "Bolt" to Icons.Outlined.Bolt,
    "Book" to Icons.Outlined.Book,
    "Bookmark" to Icons.Outlined.Bookmark,
    "Brush" to Icons.Outlined.Brush,
    "Bug" to Icons.Outlined.BugReport,
    "Build" to Icons.Outlined.Build,
    "Business" to Icons.Outlined.BusinessCenter,
    "Cake" to Icons.Outlined.Cake,
    "Calculator" to Icons.Outlined.Calculate,
    "Calendar" to Icons.Outlined.CalendarMonth,
    "Call" to Icons.Outlined.Call,
    "Cast" to Icons.Outlined.Cast,
    "Celebration" to Icons.Outlined.Celebration,
    "Chat" to Icons.AutoMirrored.Outlined.Chat,
    "Cloud" to Icons.Outlined.Cloud,
    "Code" to Icons.Outlined.Code,
    "Coffee" to Icons.Outlined.Coffee,
    "Color Lens" to Icons.Outlined.ColorLens,
    "Computer" to Icons.Outlined.Computer,
    "Construction" to Icons.Outlined.Construction,
    "Contacts" to Icons.Outlined.Contacts,
    "Credit Card" to Icons.Outlined.CreditCard,
    "Dark Mode" to Icons.Outlined.DarkMode,
    "Description" to Icons.Outlined.Description,
    "Bike" to Icons.AutoMirrored.Outlined.DirectionsBike,
    "Bus" to Icons.Outlined.DirectionsBus,
    "Car" to Icons.Outlined.DirectionsCar,
    "Run" to Icons.AutoMirrored.Outlined.DirectionsRun,
    "Walk" to Icons.AutoMirrored.Outlined.DirectionsWalk,
    "Email" to Icons.Outlined.Email,
    "Trophy" to Icons.Outlined.EmojiEvents,
    "Event" to Icons.Outlined.Event,
    "Explore" to Icons.Outlined.Explore,
    "Extension" to Icons.Outlined.Extension,
    "Fast Food" to Icons.Outlined.Fastfood,
    "Heart" to Icons.Outlined.Favorite,
    "Fingerprint" to Icons.Outlined.Fingerprint,
    "Fitness" to Icons.Outlined.FitnessCenter,
    "Flag" to Icons.Outlined.Flag,
    "Flight" to Icons.Outlined.Flight,
    "Flashlight" to Icons.Outlined.FlashlightOn,
    "Folder" to Icons.Outlined.Folder,
    "Forum" to Icons.Outlined.Forum,
    "Gavel" to Icons.Outlined.Gavel,
    "Group" to Icons.Outlined.Group,
    "Headphones" to Icons.Outlined.Headphones,
    "Home" to Icons.Outlined.Home,
    "Image" to Icons.Outlined.Image,
    "Key" to Icons.Outlined.Key,
    "Keyboard" to Icons.Outlined.Keyboard,
    "Language" to Icons.Outlined.Language,
    "Laptop" to Icons.Outlined.Laptop,
    "Library" to Icons.AutoMirrored.Outlined.LibraryBooks,
    "Music Library" to Icons.Outlined.LibraryMusic,
    "Lightbulb" to Icons.Outlined.Lightbulb,
    "Light Mode" to Icons.Outlined.LightMode,
    "Bar" to Icons.Outlined.LocalBar,
    "Cafe" to Icons.Outlined.LocalCafe,
    "Flower" to Icons.Outlined.LocalFlorist,
    "Gas Station" to Icons.Outlined.LocalGasStation,
    "Hospital" to Icons.Outlined.LocalHospital,
    "Offer" to Icons.Outlined.LocalOffer,
    "Pizza" to Icons.Outlined.LocalPizza,
    "Location" to Icons.Outlined.LocationOn,
    "Lock" to Icons.Outlined.Lock,
    "Map" to Icons.Outlined.Map,
    "Medication" to Icons.Outlined.Medication,
    "Menu Book" to Icons.AutoMirrored.Outlined.MenuBook,
    "Mic" to Icons.Outlined.Mic,
    "Heart Monitor" to Icons.Outlined.MonitorHeart,
    "Movie" to Icons.Outlined.Movie,
    "Museum" to Icons.Outlined.Museum,
    "Music Note" to Icons.Outlined.MusicNote,
    "Navigation" to Icons.Outlined.Navigation,
    "Newspaper" to Icons.Outlined.Newspaper,
    "Notifications" to Icons.Outlined.Notifications,
    "Palette" to Icons.Outlined.Palette,
    "Payments" to Icons.Outlined.Payments,
    "Person" to Icons.Outlined.Person,
    "Pets" to Icons.Outlined.Pets,
    "Camera" to Icons.Outlined.PhotoCamera,
    "Piano" to Icons.Outlined.Piano,
    "Play" to Icons.Outlined.PlayCircle,
    "Podcasts" to Icons.Outlined.Podcasts,
    "Power" to Icons.Outlined.Power,
    "Print" to Icons.Outlined.Print,
    "Psychology" to Icons.Outlined.Psychology,
    "Globe" to Icons.Outlined.Public,
    "QR Scanner" to Icons.Outlined.QrCodeScanner,
    "Queue Music" to Icons.AutoMirrored.Outlined.QueueMusic,
    "Radio" to Icons.Outlined.Radio,
    "Receipt" to Icons.Outlined.Receipt,
    "Restaurant" to Icons.Outlined.Restaurant,
    "Rocket" to Icons.Outlined.Rocket,
    "Router" to Icons.Outlined.Router,
    "Savings" to Icons.Outlined.Savings,
    "School" to Icons.Outlined.School,
    "Science" to Icons.Outlined.Science,
    "Search" to Icons.Outlined.Search,
    "Security" to Icons.Outlined.Security,
    "Meditation" to Icons.Outlined.SelfImprovement,
    "Send" to Icons.AutoMirrored.Outlined.Send,
    "Settings" to Icons.Outlined.Settings,
    "Shield" to Icons.Outlined.Shield,
    "Shopping Bag" to Icons.Outlined.ShoppingBag,
    "Shopping Cart" to Icons.Outlined.ShoppingCart,
    "Smartphone" to Icons.Outlined.Smartphone,
    "Spa" to Icons.Outlined.Spa,
    "Speaker" to Icons.Outlined.Speaker,
    "Basketball" to Icons.Outlined.SportsBasketball,
    "Games" to Icons.Outlined.SportsEsports,
    "Soccer" to Icons.Outlined.SportsSoccer,
    "Star" to Icons.Outlined.Star,
    "Storage" to Icons.Outlined.Storage,
    "Storefront" to Icons.Outlined.Storefront,
    "Tablet" to Icons.Outlined.Tablet,
    "Terminal" to Icons.Outlined.Terminal,
    "Timer" to Icons.Outlined.Timer,
    "Train" to Icons.Outlined.Train,
    "Translate" to Icons.Outlined.Translate,
    "TV" to Icons.Outlined.Tv,
    "USB" to Icons.Outlined.Usb,
    "Video" to Icons.Outlined.Videocam,
    "Eye" to Icons.Outlined.Visibility,
    "Watch" to Icons.Outlined.Watch,
    "Water Drop" to Icons.Outlined.WaterDrop,
    "Sun" to Icons.Outlined.WbSunny,
    "Wi-Fi" to Icons.Outlined.Wifi,
    "Work" to Icons.Outlined.Work,
)
