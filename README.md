## Dinner_App
	- 大部分的資料都在app > src > main > ...
	- 其餘的部分都是環境設置

### main > java > ...(主程式)
	- MainActivity.kt:	主畫面
	- LoginActivity.kt:	登入畫面
	- SignUpActivity.kt:	註冊帳號畫面
	- EditPreference.kt:	編輯喜好畫面
	- PublicComment
		- Activity.kt:	公共評論畫面(每列皆為一家餐廳)
		- Adapter.kt:	公共評論中任一餐廳的容器(包括查看該餐廳的按鈕設計)
	- Commet
		- EditActivity.kt:	個人評論編輯畫面
		- ListActivity.kt:	某一餐廳的所有評論畫面
		- ListAdapter.kt:	關於該餐廳的每則評論中任一評論的容器(包括查看該評論的按鈕設計)
	- HistoryActivity.kt:	歷史紀錄畫面

### main > res > ...
	- 各個頁面的架構及裝飾(有點像html)

### app > build > outputs > apk > debug > app-debug.apk
	- 該檔案可以下載至android手機上並使用Dinner_App 