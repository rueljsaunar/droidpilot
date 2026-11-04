import droidpilot as dp

d = dp.Device()
d.app_start("com.droidpilot.sampletarget")

d.find(dp.id("username")).wait(timeout=10000).set_text("qa_user")
d.find(dp.id("password")).set_text("secret")
d.find(dp.id("btnLogin")).click()

dp.expect(d.find(dp.id("rvItems"))).to_be_visible(timeout=10000)
