# Настройка раздельного туннелирования в различных приложениях
Раздельное туннелирование в наши дни очень важная штука! Роскомнадзор может получить ip адрес vpn от множества платформ рунета и заблокировать его. Чтобы этот процесс ему усложнить, требуется настроить раздельное туннелирование. Раздельное туннелирование позволяет пропускать трафик выбранных платформ напрямую, т.е не пропускать их через впн. В этой недоиструкции я попробую помочь вам в ее настройке.
## V2RayTun
- Перейдите в настройки. Вам сразу в глаза бросится пункт "Маршрутизация". Нажмите на нее. (Если вы пользуетесь пк версией V2RayTun, вкладка маршрутизации находится на главной странице)
- Вы попадете на страницу маршрутизации, здесь нам потребуется сделать несполько вещей.
- Сначала настроим сайты с доменом .ру.
- Нажмите на +.
<img width="289" height="640" alt="Image" src="https://github.com/user-attachments/assets/2ba8beef-02ce-4de9-9a47-bd75004497a1" />

- вы попадете на страницу настройки Р.Т. Настройте все точно также:
<img width="289" height="640" alt="Image" src="https://github.com/user-attachments/assets/d3d8b2c0-4886-439c-986a-d636d4daf4b4" />

- теперь нажмите на галочку, и все готово! Если потребуется, можно сделать абсолютно такое же правило для любого другого сайта, заменив домен .ru на домен нужного сайта.

- Следом настроим маршрутизацию приложений.
- На главной странице маршрутизации нажмите на маршрутизация выбранных приложений.
- Там включаем ее и режим обхода. Следом выбираем приложения, которые не должны работать через впн(Обязательно ВСЕ приложения ру сегмента).
- Все готово! Можете пользоваться!
## AmneziaVPN
- На главной странице нажмите на "Раздельное туннелирование ...":
<img width="289" height="640" alt="Image" src="https://github.com/user-attachments/assets/7e4a001f-b25a-4aac-8cc3-e3c7d0336f05" />

- После вы попадёте на страницу настройки. Нам понадобятся оба пункта.
- Сначала настроим Р.Т. для сайтов.
<img width="289" height="640" alt="Image" src="https://github.com/user-attachments/assets/701a11c1-2643-49dd-8fe1-ec5fe33347ea" />

- Проверьте, чтобы пункты 1-2 совпадали у вас.
- К сожалению, в AmneziaVPN нельзя настроить раздельное туннелирование для всех сайтов с доменом .ru. По этому придётся добавлять все сайты ру сегмента, которыми ВЫ пользуетесь (пометка 3).
- После добавления, возвращаемся на прошлую страницу и выбираем Р.Т. для приложений.
- Точно так же проверяем, чтобы пункты 1-2 совпадали.
- Следом нажимаем на + и выбираем приложения, которые не должны работать через впн(Обязательно ВСЕ приложения ру сегмента).
- Все готово! Можете пользоваться!
## INCY
- Тут все чуть сложнее. Перейдите в настройки.
<img width="289" height="640" alt="Image" src="https://github.com/user-attachments/assets/6519eddb-65e9-4ed6-acfc-6e9d2b5f0d8e" />

- Нам понадобятся оба выделенных пункта. Начнём с "Прокси по приложениям".
- Включаем его, выбираем режим "Обход" и выбираем приложения, которые не должны работать через впн(Обязательно ВСЕ приложения ру сегмента).
- Переходим обратно в настройки и жмем на "Туннель".
- Там мы включаем "Профили маршрутизации". Далее, для простоты, я даю вам ссылку на р.т. Ее придётся вручную скопировать и вставить на главной странице приложения, т.к гитхаб засранец не захотел делать с ней гиперссылку. incy://routing/add/eyJOYW1lIjoi0KDQvtGB0YHQuNGPIiwiR2xvYmFsUHJveHkiOnRydWUsIlJlbW90ZUROU1R5cGUiOiJEb0giLCJSZW1vdGVETlNEb21haW4iOiJodHRwczovL2Nsb3VkZmxhcmUtZG5zLmNvbS9kbnMtcXVlcnkiLCJSZW1vdGVETlNJUCI6IiIsIkRvbWVzdGljRE5TVHlwZSI6IkRvVSIsIkRvbWVzdGljRE5TRG9tYWluIjoiaHR0cHM6Ly8yMjMuNS41LjUvZG5zLXF1ZXJ5IiwiRG9tZXN0aWNETlNJUCI6IjguOC44LjgiLCJGYWtlRE5TIjp0cnVlLCJEbnNIb3N0cyI6eyJjbG91ZGZsYXJlLWRucy5jb20iOiIxLjEuMS4xIn0sIlJlbW90ZURucyI6Imh0dHBzOi8vY2xvdWRmbGFyZS1kbnMuY29tL2Rucy1xdWVyeSIsIkRvbWVzdGljRG5zIjoiOC44LjguOCIsIkdlb2lwdXJsIjoiaHR0cHM6Ly9naXRodWIuY29tL0xveWFsc29sZGllci92MnJheS1ydWxlcy1kYXQvcmVsZWFzZXMvbGF0ZXN0L2Rvd25sb2FkL2dlb2lwLmRhdCIsIkdlb3NpdGV1cmwiOiJodHRwczovL2dpdGh1Yi5jb20vTG95YWxzb2xkaWVyL3YycmF5LXJ1bGVzLWRhdC9yZWxlYXNlcy9sYXRlc3QvZG93bmxvYWQvZ2Vvc2l0ZS5kYXQiLCJMYXN0VXBkYXRlZCI6bnVsbCwiR2VvaXBIYXNoIjoiMDZjMWQ1MGQwMDEzYWY1N2ZmZTc4YzVhZjkzZjBkYzZhYTk5ODg5MGYzZWUyMzEzOTM4MTZmOTExNzgxMGM0YSIsIkdlb3NpdGVIYXNoIjoiOThiY2U2NGQ3ZDE0MzUyNDQxNGJiYzY1ZTk5OTlhNzM4ZmJiNzEyNTYzMDg4OWUxNmU1MjAwNWUxNWVhYzdjMCIsIkRpcmVjdFNpdGVzIjpbImdlb3NpdGU6Q0FURUdPUlktUlUiXSwiUHJveHlTaXRlcyI6W10sIkJsb2NrU2l0ZXMiOlsiZ2Vvc2l0ZTpjYXRlZ29yeS1hZHMiXSwiRGlyZWN0SXAiOlsiZ2VvaXA6cnUiLCJnZW9pcDpwcml2YXRlIl0sIlByb3h5SXAiOltdLCJCbG9ja0lwIjpbXSwiRG9tYWluU3RyYXRlZ3kiOiJJUElmTm9uTWF0Y2giLCJSb3V0ZU9yZGVyIjoiYmxvY2stcHJveHktZGlyZWN0IiwiVXNlQ2h1bmtGaWxlcyI6ZmFsc2UsImlzRGVmYXVsdCI6ZmFsc2V9 . Кому будет интересно, можете ознакомиться с настройкой р.т [на вики INCY](https://incy.gitbook.io/docs/dokumentaciya-dlya-razrabotchikov/marshrutizaciya-routing)
- Все готово! Можете пользоваться!

## По мере развития, здесь появится больше приложений.
