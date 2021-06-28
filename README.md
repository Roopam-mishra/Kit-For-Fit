# Kit For Fit - Health Tracking Android App
I have created this app as a POC during my internship starting phase. 
It fetches user’s data from google fitness cloud with the help of Fitness APIs (only after user's consent).

This app follows MVVM (Model-View-ViewModel) architecture. 
I have not added Runtime Permissions at this time, it only takes oAuth Permissions from the user.

This app tracks your today's data and the past week data of the following parameters:
1. Step Counts
2. Calories Expended
3. Distance Travelled 
4. Active Move Minutes
5. Heart Points earned
6. Weight


MAIN SCREEN
|:-:|
<img src="Screenshots/photo_1.jpeg" width="30%" height="580" />

| Steps Count | Calories Expended |  Distance Covered |
|:-:|:-:|:-:|
| ![Steps Count](Screenshots/photo_2.jpeg) | ![Calories Expended](Screenshots/photo_3.jpeg) | ![Distance Covered](Screenshots/photo_4.jpeg) |

| Move Minutes | Heart Points |  Weight |
|:-:|:-:|:-:|
| ![Move Minutes](Screenshots/photo_5.jpeg) | ![Heart Points](Screenshots/photo_6.jpeg) | ![Weight](Screenshots/photo_7.jpeg) |


Project purpose is to fetch data from Fitness APIs', not UI 😄.

RESOURCES/REFERENCES:
+ [Google Fit Tutorial](https://developers.google.com/fit/)
