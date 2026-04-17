<template>
  <div class="login-shell">
    <div class="login-panel">
      <div class="login-copy">
        <div class="eyebrow">智慧仓储平台</div>
        <h1>商业仓库系统</h1>
        <p>覆盖主数据、入出库、库存、预警中心与滞留监控的后台管理台。</p>
        <ul>
          <li>实时掌握库存与作业状态</li>
          <li>自动发现收货、上架、出库滞留风险</li>
          <li>支持多仓、多货主、多客户场景扩展</li>
        </ul>
      </div>

      <div class="login-form-card">
        <div class="form-title">系统登录</div>
        <el-form :model="form" label-position="top">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" @click="submit">
            登录系统
          </el-button>
          <div class="tips">默认账号：admin / admin123</div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { post } from '../api'

export default {
  name: 'LoginPage',
  data() {
    return {
      loading: false,
      form: {
        username: 'admin',
        password: 'admin123'
      }
    }
  },
  methods: {
    async submit() {
      this.loading = true
      try {
        const response = await post('/auth/login', this.form)
        localStorage.setItem('wms-user', JSON.stringify(response.data))
        this.$message.success('登录成功')
        this.$router.push('/dashboard')
      } catch (error) {
        this.$message.error(error.message || '登录失败')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(circle at 15% 20%, rgba(14, 165, 233, 0.24), transparent 26%),
    radial-gradient(circle at 82% 22%, rgba(22, 163, 74, 0.18), transparent 24%),
    linear-gradient(135deg, #e0f2fe 0%, #f8fafc 48%, #dcfce7 100%);
}

.login-panel {
  width: min(1080px, 100%);
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 24px;
  align-items: stretch;
}

.login-copy {
  background: linear-gradient(160deg, #0f172a 0%, #0369a1 55%, #065f46 100%);
  color: #fff;
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 24px 60px rgba(14, 116, 144, 0.24);
}

.login-copy .eyebrow {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #bfdbfe;
}

.login-copy h1 {
  margin: 18px 0 14px;
  font-size: 42px;
}

.login-copy p,
.login-copy li {
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.8;
}

.login-copy ul {
  padding-left: 18px;
  margin-top: 24px;
}

.login-form-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 24px;
  padding: 36px;
  box-shadow: 0 24px 50px rgba(15, 23, 42, 0.10);
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 24px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.tips {
  margin-top: 14px;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 960px) {
  .login-panel {
    grid-template-columns: 1fr;
  }
}
</style>
