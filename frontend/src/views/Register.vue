<template>
  <div class="auth-shell">
    <div class="auth-card register-card">
      <section class="auth-brand">
        <div class="brand-head">
          <div class="brand-mark">W</div>
          <div>
            <div class="brand-title">企业账号申请</div>
            <div class="brand-subtitle">Identity Provisioning</div>
          </div>
        </div>

        <div class="brand-metrics">
          <div class="metric-row">
            <span>默认角色</span>
            <strong>OPERATOR</strong>
          </div>
          <div class="metric-row">
            <span>账号状态</span>
            <strong>待启用</strong>
          </div>
          <div class="metric-row">
            <span>启用位置</span>
            <strong>权限管理</strong>
          </div>
        </div>
      </section>

      <section class="auth-form-panel">
        <div class="form-head">
          <div>
            <h1>账号申请</h1>
            <p>提交后由管理员审核并启用账号</p>
          </div>
          <router-link class="switch-link" to="/login">返回登录</router-link>
        </div>

        <el-form :model="form" label-position="top" class="auth-form">
          <div class="form-grid">
            <el-form-item label="用户名">
              <el-input
                v-model.trim="form.username"
                autocomplete="username"
                placeholder="3-32 位字母、数字或下划线"
                prefix-icon="el-icon-user"
              />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input
                v-model.trim="form.displayName"
                autocomplete="name"
                placeholder="请输入真实姓名"
                prefix-icon="el-icon-postcard"
              />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input
                v-model.trim="form.phone"
                autocomplete="tel"
                placeholder="用于管理员核验"
                prefix-icon="el-icon-phone"
              />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input
                v-model.trim="form.email"
                autocomplete="email"
                placeholder="name@example.com"
                prefix-icon="el-icon-message"
              />
            </el-form-item>
          </div>

          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              autocomplete="new-password"
              placeholder="至少 6 位"
              prefix-icon="el-icon-lock"
              show-password
              @keyup.enter.native="submit"
            />
          </el-form-item>

          <el-button type="primary" :loading="loading" class="submit-btn" @click="submit">
            提交申请
          </el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script>
import { post } from '../api'

export default {
  name: 'RegisterPage',
  data() {
    return {
      loading: false,
      form: {
        username: '',
        password: '',
        displayName: '',
        phone: '',
        email: ''
      }
    }
  },
  methods: {
    async submit() {
      if (!this.form.username || !this.form.password || !this.form.displayName) {
        this.$message.warning('请填写用户名、姓名和密码')
        return
      }
      this.loading = true
      try {
        const response = await post('/auth/register', this.form)
        this.$alert(response.data.message || '账号申请已提交，请等待管理员启用', '提交成功', {
          type: 'success',
          confirmButtonText: '返回登录',
          callback: () => this.$router.push('/login')
        })
      } catch (error) {
        this.$message.error(error.message || '提交失败')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.auth-shell {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f3f5f9;
  overflow: hidden;
}

.auth-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(37, 99, 235, 0.06) 1px, transparent 1px),
    linear-gradient(180deg, rgba(15, 23, 42, 0.05) 1px, transparent 1px);
  background-size: 52px 52px;
  -webkit-mask-image: linear-gradient(120deg, transparent 0%, #000 18%, #000 72%, transparent 100%);
  mask-image: linear-gradient(120deg, transparent 0%, #000 18%, #000 72%, transparent 100%);
  animation: gridDrift 18s linear infinite;
}

.auth-card {
  position: relative;
  z-index: 1;
  width: min(940px, calc(100vw - 48px));
  max-width: 100%;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.12);
  overflow: hidden;
  animation: authEnter 520ms ease-out both;
}

.auth-brand {
  position: relative;
  padding: 32px;
  background: #0b1220;
  border-right: 1px solid #101827;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 32px;
  overflow: hidden;
}

.auth-brand::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: -36%;
  height: 42%;
  background: linear-gradient(180deg, transparent, rgba(59, 130, 246, 0.24), transparent);
  animation: scanSweep 5.8s ease-in-out infinite;
  pointer-events: none;
}

.brand-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: #2563eb;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.36);
  animation: markPulse 3.2s ease-in-out infinite;
}

.brand-title {
  color: #ffffff;
  font-size: 18px;
  line-height: 24px;
  font-weight: 700;
}

.brand-subtitle {
  margin-top: 2px;
  color: #8a95a8;
  font-size: 12px;
}

.brand-metrics {
  position: relative;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.10);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.metric-row {
  min-height: 42px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.metric-row:last-child {
  border-bottom: none;
}

.metric-row span {
  color: #9aa4b5;
  font-size: 13px;
}

.metric-row strong {
  color: #ffffff;
  font-size: 13px;
}

.auth-form-panel {
  position: relative;
  min-width: 0;
  padding: 36px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  animation: formEnter 620ms ease-out 120ms both;
}

.form-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.form-head h1 {
  margin: 0;
  color: #101828;
  font-size: 24px;
  line-height: 30px;
  font-weight: 700;
}

.form-head p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 20px;
}

.switch-link {
  color: #2563eb;
  font-size: 13px;
  font-weight: 650;
  line-height: 30px;
  text-decoration: none;
  white-space: nowrap;
}

.switch-link:hover {
  color: #1d4ed8;
}

.auth-form {
  width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
  transition: transform 160ms ease, box-shadow 160ms ease;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.22);
}

.submit-btn:active {
  transform: translateY(0);
}

@keyframes authEnter {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes formEnter {
  from {
    opacity: 0;
    transform: translateX(12px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes scanSweep {
  0%,
  18% {
    transform: translateY(0);
    opacity: 0;
  }
  42% {
    opacity: 1;
  }
  72%,
  100% {
    transform: translateY(320%);
    opacity: 0;
  }
}

@keyframes markPulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.30);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(37, 99, 235, 0);
  }
}

@keyframes gridDrift {
  from {
    background-position: 0 0, 0 0;
  }
  to {
    background-position: 52px 0, 0 52px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-shell::before,
  .auth-card,
  .auth-brand::after,
  .auth-form-panel,
  .brand-mark {
    animation: none;
  }

  .submit-btn {
    transition: none;
  }
}

@media (max-width: 860px) {
  .auth-card {
    grid-template-columns: 1fr;
    width: min(520px, calc(100vw - 32px));
  }

  .auth-brand {
    padding: 18px 20px;
    border-right: none;
    border-bottom: 1px solid #d8dee6;
  }

  .brand-metrics {
    display: none;
  }

  .auth-form-panel {
    padding: 28px 24px 32px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
